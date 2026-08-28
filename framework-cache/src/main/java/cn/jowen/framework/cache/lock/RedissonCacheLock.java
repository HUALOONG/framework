package cn.jowen.framework.cache.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson {@link RLock} 的 {@link CacheLock} 实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class RedissonCacheLock implements CacheLock {

    private final RLock _lock;

    public RedissonCacheLock(RedissonClient client, String lockName) {
        this._lock = client.getLock(lockName);
    }

    @Override
    public boolean tryLock(Duration waitTime, @Nullable Duration leaseTime) {
        try {
            long waitMillis = waitTime.toMillis();
            if (leaseTime != null) {
                return _lock.tryLock(waitMillis, leaseTime.toMillis(), TimeUnit.MILLISECONDS);
            }
            return _lock.tryLock(waitMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock() {
        if (_lock.isHeldByCurrentThread()) {
            _lock.unlock();
        }
    }

    @Override
    public boolean isLocked() {
        return _lock.isLocked();
    }
}
