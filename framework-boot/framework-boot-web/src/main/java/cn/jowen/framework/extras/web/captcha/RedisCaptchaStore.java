package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.core.assertion.Assert;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 基于 Redis 的验证码存储（多实例部署适用）。
 *
 * <p>与 {@link CaptchaStore.InMemory} 语义对齐，差异仅在于存储位置：
 * 验证码写入 Redis 并携带 TTL，任何节点的校验请求都能读到同一份数据，
 * 因此「A 节点生成、B 节点校验」的多实例场景可直接工作。
 *
 * <p><b>序列化方案</b>：{@link Captcha} 的 {@code code / text / image / expireAt}
 * 拼接为单个字符串值，字段间以 {@code |} 分隔，各字段做 Base64 编码，
 * {@code null} 以固定标记 {@value #NULL_MARKER} 表示。Base64 字符集
 * （{@code A-Za-z0-9+/=}）不含分隔符与标记，因此编码可逆且无歧义，
 * 无需引入 JSON 依赖。
 *
 * <p><b>关于 {@code setIfAbsent}</b>：{@code save} 的语义是覆盖写，而
 * {@link RedisCommandExecutor} 只提供 {@code setIfAbsent}。此处直接复用它是安全的——
 * 验证码 {@code id} 由生成器产生且全局唯一（UUID / Snowflake），键不可能重复，
 * 因此「仅不存在时写入」与「覆盖写」在本场景下等价。这样做的好处是
 * <b>不必为单一场景扩展 SPI 接口</b>，避免对既有适配实现方造成破坏性变更。
 *
 * <p>本类<b>不依赖任何 Redis 客户端</b>，仅依赖 {@link RedisCommandExecutor} 抽象。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class RedisCaptchaStore implements CaptchaStore {

    /** 字段分隔符。Base64 字符集不含该字符。 */
    private static final char SEPARATOR = '|';

    /** 表示 {@code null} 字段的固定标记。Base64 字符集不含该字符。 */
    private static final String NULL_MARKER = "~";

    /** 合法的字段个数：code / text / image / expireAt。 */
    private static final int FIELD_COUNT = 4;

    /** 默认的 Redis key 前缀，避免与业务自有键空间冲突。 */
    private static final String DEFAULT_KEY_PREFIX = "captcha:";

    /** executor 不可变字段。 */
    private final RedisCommandExecutor executor;

    /** keyPrefix 不可变字段。 */
    private final String keyPrefix;

    /**
     * 使用默认前缀 {@code captcha:} 构造实例。
     *
     * @param executor Redis 命令执行抽象，不可为 {@code null}
     */
    public RedisCaptchaStore(RedisCommandExecutor executor) {
        this(executor, DEFAULT_KEY_PREFIX);
    }

    /**
     * 使用自定义前缀构造实例。
     *
     * @param executor  Redis 命令执行抽象，不可为 {@code null}
     * @param keyPrefix 键前缀，不可为空串（建议以分隔符结尾，如 {@code "captcha:"}）
     */
    public RedisCaptchaStore(RedisCommandExecutor executor, String keyPrefix) {
        Assert.notNull(executor, null, "executor must not be null");
        Assert.notEmpty(keyPrefix, null, "keyPrefix must not be blank");
        this.executor = executor;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 保存验证码。
     *
     * <p>过期时间取自 {@code captcha.expireAt()} 与当前时刻的差值；
     * 若验证码保存时已过期（差值 {@code <= 0}），则拒绝写入——
     * 写入也无法被正常校验，提前失败可避免向 Redis 写入注定无用的数据。
     *
     * @param captcha 验证码，不可为 {@code null}
     */
    @Override
    public void save(Captcha captcha) {
        Assert.notNull(captcha, null, "captcha must not be null");
        long ttlMillis = captcha.expireAt() - System.currentTimeMillis();
        Assert.isTrue(ttlMillis > 0, null, "captcha has already expired, refuse to store");
        executor.setIfAbsent(keyPrefix + captcha.id(), encode(captcha), ttlMillis);
    }

    /**
     * 读取验证码（不移除）。
     *
     * <p>已过期但未过 Redis TTL 的条目由调用方通过 {@link Captcha#expired()} 判断，
     * 本方法不做过滤，保持「读取」与「判断」职责分离。
     *
     * @param id 标识（不含前缀）
     * @return 验证码，不存在返回 {@code null}
     */
    @Override
    public Captcha get(String id) {
        Assert.notNull(id, null, "id must not be null");
        String raw = executor.get(keyPrefix + id);
        if (raw == null) {
            return null;
        }
        return decode(id, raw);
    }

    /**
     * 移除验证码。
     *
     * @param id 标识（不含前缀）
     */
    @Override
    public void remove(String id) {
        Assert.notNull(id, null, "id must not be null");
        executor.delete(keyPrefix + id);
    }

    /**
     * 将验证码编码为可存储的字符串。
     *
     * @param captcha 验证码
     * @return 编码结果
     */
    private static String encode(Captcha captcha) {
        return encodeText(captcha.code()) + SEPARATOR
                + encodeText(captcha.text()) + SEPARATOR
                + encodeImage(captcha.image()) + SEPARATOR
                + captcha.expireAt();
    }

    /**
     * 将字符串字段 Base64 编码；{@code null} 编码为 {@value #NULL_MARKER}。
     *
     * @param value 字段值，可为 {@code null}
     * @return 编码结果
     */
    private static String encodeText(@Nullable String value) {
        if (value == null) {
            return NULL_MARKER;
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 将字节数组字段 Base64 编码；{@code null} 编码为 {@value #NULL_MARKER}。
     *
     * @param value 字段值，可为 {@code null}
     * @return 编码结果
     */
    private static String encodeImage(@Nullable byte[] value) {
        if (value == null) {
            return NULL_MARKER;
        }
        return Base64.getEncoder().encodeToString(value);
    }

    /**
     * 将存储字符串解码为验证码。
     *
     * <p>{@code id} 单独由参数传入而非编进 payload：它同时是 Redis 的 key，
     * 编进 value 属于冗余存储；由 {@code get(id)} 透传可保持编码格式最小化。
     *
     * @param id  验证码标识（不含前缀），由调用方从 Redis key 还原
     * @param raw 编码结果
     * @return 验证码
     * @throws IllegalStateException 编码格式损坏（字段数不符或字段无法解析）
     */
    private static Captcha decode(String id, String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != FIELD_COUNT) {
            throw new IllegalStateException(
                    "corrupted captcha payload: expected " + FIELD_COUNT + " fields but got " + parts.length);
        }
        try {
            return new Captcha(id, decodeText(parts[0]), decodeText(parts[1]),
                    decodeImage(parts[2]), Long.parseLong(parts[3]));
        } catch (IllegalArgumentException ex) {
            // Base64 解码失败或时间戳非数字，统一收敛为格式损坏异常，避免泄漏实现细节。
            throw new IllegalStateException("corrupted captcha payload: " + id, ex);
        }
    }

    /**
     * 解码字符串字段；{@value #NULL_MARKER} 解码为 {@code null}。
     *
     * @param encoded 编码结果
     * @return 原始值，可为 {@code null}
     */
    private static @Nullable String decodeText(String encoded) {
        if (NULL_MARKER.equals(encoded)) {
            return null;
        }
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    /**
     * 解码字节数组字段；{@value #NULL_MARKER} 解码为 {@code null}。
     *
     * @param encoded 编码结果
     * @return 原始值，可为 {@code null}
     */
    private static @Nullable byte[] decodeImage(String encoded) {
        if (NULL_MARKER.equals(encoded)) {
            return null;
        }
        return Base64.getDecoder().decode(encoded);
    }
}
