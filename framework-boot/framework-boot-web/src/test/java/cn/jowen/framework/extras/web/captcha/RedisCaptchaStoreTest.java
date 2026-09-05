package cn.jowen.framework.extras.web.captcha;

import cn.jowen.framework.core.exception.BusinessException;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RedisCaptchaStore} 单元测试。
 *
 * <p>编解码的正确性只能用「写入再读出」的方式验证，因此这里用内存版
 * {@link RedisCommandExecutor} 作为测试替身，而不是 Mockito mock——
 * mock 只能验证「调用了什么」，无法验证「编码/解码是否真的可逆」。
 *
 * <p>覆盖场景：全字段往返、可空字段往返、键前缀、TTL 传递、过期拒写、
 * 格式损坏容错、参数校验。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class RedisCaptchaStoreTest {

    private RecordingExecutor executor;
    private RedisCaptchaStore store;

    @BeforeEach
    void setUp() {
        executor = new RecordingExecutor();
        store = new RedisCaptchaStore(executor);
    }

    /** 全字段写入后读出，四个字段必须逐字一致。 */
    @Test
    void saveAndGetRoundTripsAllFields() {
        long expireAt = System.currentTimeMillis() + 60_000L;
        Captcha captcha = new Captcha("c-1", "7B2F", "3+4=?",
                new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0xFF}, expireAt);

        store.save(captcha);
        Captcha loaded = store.get("c-1");

        assertThat(loaded).isNotNull();
        assertThat(loaded.id()).isEqualTo("c-1");
        assertThat(loaded.code()).isEqualTo("7B2F");
        assertThat(loaded.text()).isEqualTo("3+4=?");
        assertThat(loaded.image()).containsExactly((byte) 0x01, (byte) 0x02, (byte) 0xFF);
        assertThat(loaded.expireAt()).isEqualTo(expireAt);
    }

    /** 题目与图片均可空，可空字段必须无损往返（不能变成空串或 "null"）。 */
    @Test
    void saveAndGetRoundTripsWithNullableFields() {
        Captcha captcha = new Captcha("c-2", "9", null, null, System.currentTimeMillis() + 60_000L);

        store.save(captcha);
        Captcha loaded = store.get("c-2");

        assertThat(loaded).isNotNull();
        assertThat(loaded.code()).isEqualTo("9");
        assertThat(loaded.text()).isNull();
        assertThat(loaded.image()).isNull();
    }

    /** 中文等 Unicode 内容必须无损，验证编码使用 UTF-8 而非平台默认字符集。 */
    @Test
    void saveAndGetRoundTripsChineseText() {
        Captcha captcha = new Captcha("c-cn", "15", "计算：12 + 3 = ?", null,
                System.currentTimeMillis() + 60_000L);

        store.save(captcha);
        Captcha loaded = store.get("c-cn");

        assertThat(loaded).isNotNull();
        assertThat(loaded.text()).isEqualTo("计算：12 + 3 = ?");
        assertThat(loaded.code()).isEqualTo("15");
    }

    /** 键不存在时返回 null，调用方据此判定校验失败。 */
    @Test
    void getReturnsNullWhenAbsent() {
        assertThat(store.get("never-saved")).isNull();
    }

    /** 移除后不应再能读到，实现验证码的一次性语义。 */
    @Test
    void removeMakesCaptchaUnavailable() {
        store.save(new Captcha("c-3", "x", "t", null, System.currentTimeMillis() + 60_000L));
        assertThat(store.get("c-3")).isNotNull();

        store.remove("c-3");

        assertThat(store.get("c-3")).isNull();
        assertThat(executor.data()).doesNotContainKey("captcha:c-3");
    }

    /** 自定义前缀应同时作用于写入与读取，且不与默认前缀混淆。 */
    @Test
    void customKeyPrefixIsAppliedOnBothWriteAndRead() {
        RedisCaptchaStore custom = new RedisCaptchaStore(executor, "tenant-a:captcha:");
        Captcha captcha = new Captcha("c-4", "CODE", "q", null, System.currentTimeMillis() + 60_000L);

        custom.save(captcha);

        assertThat(executor.data()).containsKey("tenant-a:captcha:c-4");
        assertThat(executor.data()).doesNotContainKey("captcha:c-4");
        assertThat(custom.get("c-4")).isNotNull();
    }

    /** TTL 必须取「过期时刻 − 当前时刻」，而不是硬编码固定值。 */
    @Test
    void savePassesRemainingTimeAsTtl() {
        long now = System.currentTimeMillis();
        long expireAt = now + 30_000L;

        store.save(new Captcha("c-5", "v", "t", null, expireAt));

        long ttl = executor.lastExpireMillis();
        // 允许 100ms 的调度误差，避免测试因时序抖动而误报。
        assertThat(ttl).isBetween(29_000L, 30_000L);
    }

    /** 已过期验证码写入没有意义，必须提前拒绝而非写入注定无用的数据。 */
    @Test
    void saveRejectsAlreadyExpiredCaptcha() {
        Captcha expired = new Captcha("c-6", "dead", "t", null, System.currentTimeMillis() - 1000L);

        assertThatThrownBy(() -> store.save(expired))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("expired");
        assertThat(executor.data()).isEmpty();
    }

    /** 载荷字段数损坏时给出明确的格式异常，不能静默返回错误数据。 */
    @Test
    void getThrowsOnPayloadWithWrongFieldCount() {
        executor.data().put("captcha:c-7", "only-one-field");

        assertThatThrownBy(() -> store.get("c-7"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("corrupted");
    }

    /** 时间戳字段非数字时同样视为载荷损坏，且不泄漏底层异常类型。 */
    @Test
    void getThrowsOnUnparseableTimestamp() {
        executor.data().put("captcha:c-8", "a|b|~|not-a-number");

        assertThatThrownBy(() -> store.get("c-8"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("corrupted");
    }

    /** 注入 null 执行器属于装配错误，必须在构造期快速失败。 */
    @Test
    void constructorRejectsNullExecutor() {
        assertThatThrownBy(() -> new RedisCaptchaStore(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("executor");
    }

    /** 移除 null 标识属于编程错误，必须拒绝。 */
    @Test
    void removeRejectsNullId() {
        assertThatThrownBy(() -> store.remove(null)).isInstanceOf(BusinessException.class);
    }

    /**
     * 内存版 {@link RedisCommandExecutor}：记录写入的键值与最近一次 TTL，
     * 供 round-trip 断言使用。
     */
    private static final class RecordingExecutor implements RedisCommandExecutor {

        /** 键值存储。 */
        private final Map<String, String> store = new HashMap<>();

        /** 最近一次 setIfAbsent 收到的过期毫秒数。 */
        private long lastExpireMillis;

        /** @return 底层键值映射 */
        Map<String, String> data() {
            return store;
        }

        /** @return 最近一次写入的过期毫秒数 */
        long lastExpireMillis() {
            return lastExpireMillis;
        }

        @Override
        public boolean setIfAbsent(String key, String value, long expireMillis) {
            lastExpireMillis = expireMillis;
            return store.putIfAbsent(key, value) == null;
        }

        @Override
        public @Nullable String get(String key) {
            return store.get(key);
        }

        @Override
        public void delete(String key) {
            store.remove(key);
        }

        @Override
        public boolean deleteIfMatch(String key, String value) {
            return store.remove(key, value);
        }
    }
}
