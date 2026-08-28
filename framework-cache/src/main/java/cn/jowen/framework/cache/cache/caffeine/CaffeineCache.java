package cn.jowen.framework.cache.cache.caffeine;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheStats;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * 基于 Caffeine 的本地缓存实现，支持容量上限与 TTL 淘汰，并记录命中/未命中统计。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class CaffeineCache<K, V> implements Cache<K, V> {

    private final String _name;
    private final com.github.benmanes.caffeine.cache.Cache<K, V> _delegate;
    private final CacheStats _stats = new CacheStats();

    public CaffeineCache(String name, CacheConfiguration config) {
        this._name = name;
        this._delegate = buildCache(config);
    }

    public static <K, V> CaffeineCache<K, V> create(String name, CacheConfiguration config) {
        return new CaffeineCache<>(name, config);
    }

    private static <K, V> com.github.benmanes.caffeine.cache.Cache<K, V> buildCache(CacheConfiguration config) {
        var builder = Caffeine.newBuilder()
                .maximumSize(config.maxSize() > 0 ? config.maxSize() : Long.MAX_VALUE);
        if (config.expireAfterWrite() != null) {
            builder = builder.expireAfterWrite(config.expireAfterWrite());
        }
        if (config.expireAfterAccess() != null) {
            builder = builder.expireAfterAccess(config.expireAfterAccess());
        }
        return builder.build();
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public @Nullable V get(K key) {
        V value = _delegate.getIfPresent(key);
        if (value == null) {
            _stats.recordMiss();
        } else {
            _stats.recordHit();
        }
        return value;
    }

    @Override
    public Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    @Override
    public void put(K key, V value) {
        _delegate.put(key, value);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (_delegate.getIfPresent(key) != null) {
            return false;
        }
        _delegate.put(key, value);
        return true;
    }

    @Override
    public void evict(K key) {
        _delegate.invalidate(key);
    }

    @Override
    public void clear() {
        _delegate.invalidateAll();
    }

    @Override
    public long size() {
        return _delegate.estimatedSize();
    }

    @Override
    public CacheStats stats() {
        return _stats;
    }
}
