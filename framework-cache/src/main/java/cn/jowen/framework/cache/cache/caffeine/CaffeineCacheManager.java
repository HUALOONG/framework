package cn.jowen.framework.cache.cache.caffeine;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caffeine 一级本地缓存的 {@link CacheManager} 实现。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class CaffeineCacheManager implements CacheManager {

    private final CacheConfiguration _config;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    public CaffeineCacheManager() {
        this(CacheConfiguration.builder().build());
    }

    public CaffeineCacheManager(CacheConfiguration config) {
        this._config = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> CaffeineCache.create(n, _config));
    }

    @Override
    public @Nullable CacheStats getStats(String name) {
        Cache<?, ?> cache = caches.get(name);
        return cache == null ? null : cache.stats();
    }

    @Override
    public Iterable<String> cacheNames() {
        return caches.keySet();
    }

    @Override
    public void register(Cache<?, ?> cache) {
        caches.put(cache.name(), cache);
    }

    @Override
    public Map<String, Cache<?, ?>> asMap() {
        return caches;
    }
}