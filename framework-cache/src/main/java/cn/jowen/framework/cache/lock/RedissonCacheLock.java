package cn.jowen.framework.cache.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Redisson 分布式锁实现（预留，当前使用 ReentrantLock 单机兜底）。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class RedissonCacheLock implements CacheLock {

    private final String lockName;
    private final ReentrantLock lock = new ReentrantLock();
    private boolean locked = false;

    public RedissonCacheLock(String lockName) {
        this.lockName = lockName;
    }

    @Override
    public boolean tryLock(Duration waitTime, @Nullable Duration leaseTime) {
        try {
            return lock.tryLock(waitTime.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock() {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            locked = false;
        }
    }

    @Override
    public boolean isLocked() {
        return lock.isLocked();
    }

    public String getLockName() {
        return lockName;
    }
}
