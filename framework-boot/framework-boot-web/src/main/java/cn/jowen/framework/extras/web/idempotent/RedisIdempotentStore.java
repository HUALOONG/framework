package cn.jowen.framework.extras.web.idempotent;

import cn.jowen.framework.core.assertion.Assert;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的幂等指纹存储（多实例部署适用）。
 *
 * <p>与 {@link LocalIdempotentStore} 语义对齐，差异仅在于存储位置：
 * <ul>
 *   <li>标记动作委托 {@link RedisCommandExecutor#setIfAbsent(String, String, long)}
 *       实现，天然具备跨节点互斥能力；</li>
 *   <li>过期由 Redis 自身的 TTL 处理，因此本实现无需像本地实现那样手写
 *       过期重放逻辑——key 过期后自动消失，下一次 {@code setIfAbsent} 自然成功；</li>
 *   <li>重启不会丢失标记，服务滚动发布期间的重复请求仍能被拦截。</li>
 * </ul>
 *
 * <p>本类<b>不依赖任何 Redis 客户端</b>，仅依赖 {@link RedisCommandExecutor} 抽象，
 * 由业务方或自动装配层注入具体实现（Jedis / Lettuce / StringRedisTemplate 均可）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class RedisIdempotentStore implements IdempotentStore {

    /** 指纹占位值。幂等判断只关心「键是否存在」，值本身无语义。 */
    private static final String MARK_VALUE = "1";

    /** 默认的 Redis key 前缀，避免与业务自有键空间冲突。 */
    private static final String DEFAULT_KEY_PREFIX = "idempotent:";

    /** executor 不可变字段。 */
    private final RedisCommandExecutor executor;

    /** keyPrefix 不可变字段。 */
    private final String keyPrefix;

    /**
     * 使用默认前缀 {@code idempotent:} 构造实例。
     *
     * @param executor Redis 命令执行抽象，不可为 {@code null}
     */
    public RedisIdempotentStore(RedisCommandExecutor executor) {
        this(executor, DEFAULT_KEY_PREFIX);
    }

    /**
     * 使用自定义前缀构造实例。
     *
     * @param executor  Redis 命令执行抽象，不可为 {@code null}
     * @param keyPrefix 键前缀，不可为空串（建议以分隔符结尾，如 {@code "idempotent:"}）
     */
    public RedisIdempotentStore(RedisCommandExecutor executor, String keyPrefix) {
        Assert.notNull(executor, null, "executor must not be null");
        Assert.notEmpty(keyPrefix, null, "keyPrefix must not be blank");
        this.executor = executor;
        this.keyPrefix = keyPrefix;
    }

    /**
     * 尝试记录指纹。仅当指纹不存在时写入成功。
     *
     * @param key    幂等 key（不含前缀）
     * @param expire 过期时间
     * @param unit   时间单位
     * @return {@code true} 表示首次写入（放行），{@code false} 表示已存在（拒绝）
     */
    @Override
    public boolean tryMark(String key, long expire, TimeUnit unit) {
        Assert.notNull(key, null, "key must not be null");
        Assert.notNull(unit, null, "unit must not be null");
        Assert.isTrue(expire > 0, null, "expire must be positive");
        return executor.setIfAbsent(keyPrefix + key, MARK_VALUE, unit.toMillis(expire));
    }

    /**
     * 移除指纹。
     *
     * @param key 幂等 key（不含前缀）
     */
    @Override
    public void remove(String key) {
        Assert.notNull(key, null, "key must not be null");
        executor.delete(keyPrefix + key);
    }
}
