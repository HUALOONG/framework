package cn.jowen.framework.cache.cache.redisson;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redisson 二级分布式缓存的 {@link CacheManager} 实现。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class RedissonCacheManager implements CacheManager {

    private final RedissonClient _client;
    private final CacheConfiguration _config;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    public RedissonCacheManager(RedissonClient client) {
        this(client, CacheConfiguration.builder().build());
    }

    public RedissonCacheManager(RedissonClient client, CacheConfiguration config) {
        this._client = client;
        this._config = config;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(
                name, n -> RedissonCache.create(_client, n, _config));
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