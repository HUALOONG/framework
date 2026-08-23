package cn.jowen.framework.cache;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 默认缓存管理器。按名称缓存 {@link Cache} 实例，支持本地与组合缓存混用。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class DefaultCacheManager implements CacheManager {

    private final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    @Override
    public <K, V> Cache<K, V> getCache(String name) {
        return (Cache<K, V>) caches.computeIfAbsent(name, LocalCache::new);
    }

    @Override
    public @Nullable CacheStats getStats(String name) {
        Cache<?, ?> cache = caches.get(name);
        return cache == null ? null : cache.stats();
    }

    @Override
    public Iterable<String> cacheNames() {
        Collection<String> names = caches.keySet();
        return names::iterator;
    }

    /**
     * 注册外部构建的缓存（如组合缓存），便于统一管理。
     */
    public void register(Cache<?, ?> cache) {
        caches.put(cache.name(), cache);
    }

    /**
     * 供 Map 兼容场景使用。
     */
    public Map<String, Cache<?, ?>> asMap() {
        return caches;
    }
}
