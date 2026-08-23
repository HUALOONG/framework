package cn.jowen.framework.cache.lock;

import cn.jowen.framework.cache.api.Cache;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存锁工厂（预留实现，待接入 Redisson 分布式锁）。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class CacheLockFactory {

    private final Map<String, CacheLock> locks = new ConcurrentHashMap<>();

    /**
     * 获取缓存锁。
     *
     * @param lockName 锁名称，不可为 {@code null}
     * @return 缓存锁实例，不可为 {@code null}
     */
    public CacheLock getLock(String lockName) {
        return locks.computeIfAbsent(lockName, name -> new RedissonCacheLock(name));
    }

    /**
     * 释放缓存锁。
     *
     * @param lock 缓存锁，不可为 {@code null}
     */
    public void releaseLock(CacheLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }
}
