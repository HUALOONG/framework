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
 * 基于 {@link CaffeineCache} 的 {@link CacheManager} 实现，支持按需创建命名缓存并附加构建器配置。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class CaffeineCacheManager implements CacheManager {

    private final CacheConfiguration _config;
    private CaffeineConfigurer _configurer;
    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    public CaffeineCacheManager() {
        this(CacheConfiguration.builder().build());
    }

    public CaffeineCacheManager(CacheConfiguration config) {
        this._config = config;
    }

    public static CacheManager createDefault() {
        return new CaffeineCacheManager();
    }

    public void configure(CaffeineConfigurer configurer) {
        this._configurer = configurer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name, n -> {
            CaffeineCache<K, V> cache = CaffeineCache.create(n, _config);
            if (_configurer != null) {
                _configurer.configure(
                        com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                                .maximumSize(_config.maxSize() > 0 ? _config.maxSize() : Long.MAX_VALUE));
            }
            return cache;
        });
    }

    public <K, V> Cache<K, V> create(String name, CacheConfiguration config) {
        CaffeineCache<K, V> cache = CaffeineCache.create(name, config);
        caches.put(name, cache);
        return cache;
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
