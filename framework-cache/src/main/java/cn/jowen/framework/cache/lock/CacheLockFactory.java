package cn.jowen.framework.cache.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缓存锁工厂，基于 Redisson 按锁名创建并缓存 {@link CacheLock} 实例。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class CacheLockFactory {

    private final RedissonClient _client;
    private final Map<String, CacheLock> _locks = new ConcurrentHashMap<>();

    public CacheLockFactory(RedissonClient client) {
        this._client = client;
    }

    public CacheLock getLock(String lockName) {
        return _locks.computeIfAbsent(lockName, name -> new RedissonCacheLock(_client, name));
    }

    public void releaseLock(@Nullable CacheLock lock) {
        if (lock != null) {
            lock.unlock();
        }
    }
}
