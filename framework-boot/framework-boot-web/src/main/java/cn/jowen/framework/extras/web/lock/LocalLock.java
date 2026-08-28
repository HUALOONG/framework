package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于 JVM 内存的本地锁实现（单实例适用，重启即失效）。
 *
 * <p>以 {@code key} 维度维护 {@link ReentrantLock}，支持公平/非公平策略。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LocalLock implements Lock {

    /** LOCKS 常量。 */
    private static final ConcurrentHashMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    /** key 不可变字段。 */
    private final String key;
    /** waitMillis 不可变字段。 */
    private final long waitMillis;
    /** delegate 不可变字段。 */
    private final ReentrantLock delegate;

    /**
     * 构造实例。
     * @param key 参数 key
     * @param waitMillis 参数 waitMillis
     */
    public LocalLock(String key, long waitMillis) {
        this.key = key;
        this.waitMillis = waitMillis;
        this.delegate = LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * 执行lock操作。
     */
    @Override
    public void lock() {
        boolean acquired;
        try {
            acquired = waitMillis <= 0
                    ? delegate.tryLock()
                    : delegate.tryLock(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquireException("获取本地锁被中断: " + key, e);
        }
        if (!acquired) {
            throw new LockAcquireException("获取本地锁超时: " + key);
        }
    }

    /**
     * 执行close操作。
     */
    @Override
    public void close() {
        if (delegate.isHeldByCurrentThread()) {
            delegate.unlock();
        }
    }
}
