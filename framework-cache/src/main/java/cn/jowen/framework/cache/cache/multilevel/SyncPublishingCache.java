package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 多级缓存装饰器：在本地写/失效的同时，通过 {@link CacheSyncBroadcaster} 广播变更，
 * 实现跨节点一致性。读取与统计直接透传底层缓存。
 *
 * <p>回环防护：远端事件由 {@link RedissonCacheSyncListener} 直接写入底层多级缓存（不经过本装饰器），
 * 因此不会再次触发广播。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SyncPublishingCache implements Cache<Object, Object> {

    private final Cache<Object, Object> delegate;
    private final CacheSyncBroadcaster broadcaster;

    public SyncPublishingCache(Cache<Object, Object> delegate, CacheSyncBroadcaster broadcaster) {
        this.delegate = delegate;
        this.broadcaster = broadcaster;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public @Nullable Object get(Object key) {
        return delegate.get(key);
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
        broadcaster.publishPut((String) key, value);
    }

    @Override
    public void put(Object key, Object value, @Nullable Duration ttl) {
        delegate.put(key, value, ttl);
        broadcaster.publishPut((String) key, value);
    }

    @Override
    public boolean putIfAbsent(Object key, Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
        broadcaster.publishEvict((String) key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public long size() {
        return delegate.size();
    }

    @Override
    public CacheStats stats() {
        return delegate.stats();
    }
}
