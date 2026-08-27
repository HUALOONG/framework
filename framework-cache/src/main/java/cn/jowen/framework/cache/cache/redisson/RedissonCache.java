package cn.jowen.framework.cache.cache.redisson;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.io.Serializable;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson {@code RMapCache} 的分布式缓存实现，支持逐键独立 TTL 与容量上限。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class RedissonCache<K extends Serializable, V extends Serializable> implements Cache<K, V> {

    private final String _name;
    private final RMapCache<K, V> _mapCache;
    private final long _defaultTtlMillis;
    private final CacheStats _stats = new CacheStats();

    public RedissonCache(RedissonClient client, String name, CacheConfiguration config) {
        this._name = name;
        this._mapCache = client.getMapCache(name);
        this._defaultTtlMillis = config.expireAfterWrite() != null
                ? config.expireAfterWrite().toMillis() : -1;
    }

    public static <K extends Serializable, V extends Serializable> RedissonCache<K, V> create(
            RedissonClient client, String name, CacheConfiguration config) {
        return new RedissonCache<>(client, name, config);
    }

    @Override
    public String name() {
        return _name;
    }

    @Override
    public @Nullable V get(K key) {
        V value = _mapCache.get(key);
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
        putWithTtl(key, value, _defaultTtlMillis);
    }

    @Override
    public void put(K key, V value, @Nullable Duration ttl) {
        long ttlMillis = ttl != null ? ttl.toMillis() : _defaultTtlMillis;
        putWithTtl(key, value, ttlMillis);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (_mapCache.get(key) != null) {
            return false;
        }
        putWithTtl(key, value, _defaultTtlMillis);
        return true;
    }

    @Override
    public void evict(K key) {
        _mapCache.remove(key);
    }

    @Override
    public void clear() {
        _mapCache.clear();
    }

    @Override
    public long size() {
        return _mapCache.size();
    }

    @Override
    public CacheStats stats() {
        return _stats;
    }

    private void putWithTtl(K key, V value, long ttlMillis) {
        if (ttlMillis > 0) {
            _mapCache.fastPut(key, value, ttlMillis, TimeUnit.MILLISECONDS);
        } else {
            _mapCache.put(key, value);
        }
    }
}
