package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * 基于 Redis 的分布式锁实现（需注入 {@link RedisCommandExecutor} 适配）。
 *
 * <p>加锁使用 {@code SET key value NX PX lease}，解锁时校验 value 一致后删除，
 * 避免误释放其他线程持有的锁。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class RedisDistributedLock implements DistributedLock {

    /** executor 不可变字段。 */
    private final RedisCommandExecutor executor;
    /** defaultLeaseMillis 不可变字段。 */
    private final long defaultLeaseMillis;

    /**
     * 构造实例。
     * @param executor 参数 executor
     * @param defaultLeaseMillis 参数 defaultLeaseMillis
     */
    public RedisDistributedLock(RedisCommandExecutor executor, long defaultLeaseMillis) {
        this.executor = executor;
        this.defaultLeaseMillis = defaultLeaseMillis;
    }

    /**
     * 执行acquire操作。
     * @param key 参数 key
     * @param waitMillis 参数 waitMillis
     * @param leaseMillis 参数 leaseMillis
     * @return 结果
     */
    @Override
    public Lock acquire(String key, long waitMillis, long leaseMillis) {
        return new RedisLock(executor, key, waitMillis,
                leaseMillis <= 0 ? defaultLeaseMillis : leaseMillis);
    }

    /**
     * 「RedisLock」封装相关能力。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    private static final class RedisLock implements Lock {
        /** executor 不可变字段。 */
        private final RedisCommandExecutor executor;
        /** key 不可变字段。 */
        private final String key;
        /** value 不可变字段。 */
        private final String value;
        /** waitMillis 不可变字段。 */
        private final long waitMillis;
        /** leaseMillis 不可变字段。 */
        private final long leaseMillis;

        RedisLock(RedisCommandExecutor executor, String key, long waitMillis, long leaseMillis) {
            this.executor = executor;
            this.key = key;
            this.waitMillis = waitMillis;
            this.leaseMillis = leaseMillis;
            this.value = UUID.randomUUID().toString();
        }

        /**
         * 执行lock操作。
         */
        @Override
        public void lock() {
            long deadline = System.currentTimeMillis() + Math.max(0, waitMillis);
            do {
                if (executor.setIfAbsent(key, value, leaseMillis)) {
                    return;
                }
                try {
                    Thread.sleep(20L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new LockAcquireException("获取分布式锁被中断: " + key, e);
                }
            } while (System.currentTimeMillis() < deadline);
            throw new LockAcquireException("获取分布式锁超时: " + key);
        }

        /**
         * 执行close操作。
         */
        @Override
        public void close() {
            executor.deleteIfMatch(key, value);
        }
    }
}
