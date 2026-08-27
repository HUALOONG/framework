package cn.jowen.framework.cache.api;

import cn.jowen.framework.cache.cache.caffeine.CaffeineCache;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认 {@link CacheManager} 实现，基于 Caffeine 本地缓存按需创建命名缓存实例。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class DefaultCacheManager implements CacheManager {

    private final CacheConfiguration _config;
    private final Map<String, Cache<?, ?>> _caches = new ConcurrentHashMap<>();

    public DefaultCacheManager() {
        this(CacheConfiguration.builder().build());
    }

    public DefaultCacheManager(CacheConfiguration config) {
        this._config = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) _caches.computeIfAbsent(
                name, n -> CaffeineCache.create(n, _config));
    }

    @Override
    public @Nullable CacheStats getStats(String name) {
        Cache<?, ?> cache = _caches.get(name);
        return cache == null ? null : cache.stats();
    }

    @Override
    public Iterable<String> cacheNames() {
        Collection<String> names = _caches.keySet();
        return names::iterator;
    }

    @Override
    public void register(Cache<?, ?> cache) {
        _caches.put(cache.name(), cache);
    }

    @Override
    public Map<String, Cache<?, ?>> asMap() {
        return _caches;
    }
}
