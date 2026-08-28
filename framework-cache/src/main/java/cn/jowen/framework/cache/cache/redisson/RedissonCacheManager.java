package cn.jowen.framework.cache.cache.redisson;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RedissonClient;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 {@link RedissonCache} 的 {@link CacheManager} 实现，按需创建命名分布式缓存实例。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(
                name, n -> (Cache) RedissonCache.create(_client, n, _config));
    }

    public <K extends Serializable, V extends Serializable> Cache<K, V> create(String name, CacheConfiguration config) {
        Cache<K, V> cache = RedissonCache.create(_client, name, config);
        caches.put(name, cache);
        return cache;
    }

    public void shutdown() {
        _client.shutdown();
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
