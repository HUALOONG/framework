package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的 {@link RedisCommandExecutor} 适配实现。
 *
 * <p><b>存在意义</b>：{@code RedisDistributedLock} 已完整实现加锁/释放逻辑，但依赖
 * {@link RedisCommandExecutor} 适配具体的 Redis 客户端。本类补齐 Redisson 适配，
 * 使 {@code @Lockable(type = REDIS)} 在引入 Redisson 后真正生效，而非静默降级为本地锁。
 *
 * <p><b>为什么放在装配层</b>：Redisson 是 optional 依赖，本类必须位于
 * {@code framework-boot-autoconfigure}，由 {@code @ConditionalOnClass} 保护，
 * 确保未引入 Redisson 的应用不会因类加载失败而启动报错。
 *
 * <p>释放锁使用 Lua 脚本保证「校验值 + 删除」的原子性，避免误释放其他线程持有的锁。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class RedissonCommandExecutor implements RedisCommandExecutor {

    /**
     * 释放锁脚本：仅当键值与期望值一致时删除，避免误删他人锁。
     *
     * <p>返回值：1 表示删除成功，0 表示值不匹配或键不存在。
     */
    private static final String DELETE_IF_MATCH_SCRIPT =
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) "
                    + "else return 0 end";

    /** client 不可变字段。 */
    private final RedissonClient client;

    /**
     * 创建适配器。
     *
     * @param client Redisson 客户端，不可为 {@code null}
     */
    public RedissonCommandExecutor(RedissonClient client) {
        this.client = client;
    }

    /**
     * 设置if absent。
     * @param key 参数 key
     * @param value 参数 value
     * @param expireMillis 参数 expireMillis
     * @return 结果
     */
    @Override
    public boolean setIfAbsent(String key, String value, long expireMillis) {
        RBucket<String> bucket = client.getBucket(key);
        return bucket.trySet(value, expireMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取。
     * @param key 参数 key
     * @return 结果
     */
    @Override
    public @Nullable String get(String key) {
        return client.<String>getBucket(key).get();
    }

    /**
     * 执行delete操作。
     * @param key 参数 key
     */
    @Override
    public void delete(String key) {
        client.<String>getBucket(key).delete();
    }

    /**
     * 执行delete if match操作。
     * @param key 参数 key
     * @param value 参数 value
     * @return 结果
     */
    @Override
    public boolean deleteIfMatch(String key, String value) {
        Long affected = client.getScript().eval(
                RScript.Mode.READ_WRITE,
                DELETE_IF_MATCH_SCRIPT,
                RScript.ReturnType.LONG,
                List.of(key),
                value);
        return affected != null && affected > 0L;
    }
}
