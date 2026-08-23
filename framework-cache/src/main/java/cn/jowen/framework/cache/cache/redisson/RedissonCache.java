package cn.jowen.framework.cache.cache.redisson;

import cn.jowen.framework.cache.AbstractCache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.io.Serializable;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 基于 Redisson 的二级分布式缓存实现。支持逐键独立 TTL、容量上限与 LRU 淘汰。
 *
 * <p>键与值必须实现 {@link Serializable}，由 Redisson 负责序列化传输。
 *
 * @param <K> 键类型，必须实现 {@code Serializable}
 * @param <V> 值类型，必须实现 {@code Serializable}
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class RedissonCache<K extends Serializable, V extends Serializable> extends AbstractCache<K, V> {

    private final RMapCache<K, V> _mapCache;
    private final long _defaultTtlMillis;

    public RedissonCache(RedissonClient client, String name, CacheConfiguration config) {
        super(name);
        this._mapCache = client.getMapCache(name);
        this._defaultTtlMillis = config.expireAfterWrite() != null
                ? config.expireAfterWrite().toMillis() : -1;
    }

    /**
     * 创建 Redisson 缓存实例。
     *
     * @param client Redisson 客户端，不可为 {@code null}
     * @param name   缓存名称，不可为 {@code null}
     * @param config 缓存配置，不可为 {@code null}
     * @return 缓存实例，不可为 {@code null}
     */
    public static <K extends Serializable, V extends Serializable> RedissonCache<K, V> create(
            RedissonClient client, String name, CacheConfiguration config) {
        return new RedissonCache<>(client, name, config);
    }

    @Override
    protected @Nullable V doGet(K key) {
        return _mapCache.get(key);
    }

    @Override
    protected void doPut(K key, V value) {
        putWithTtl(key, value, _defaultTtlMillis);
    }

    @Override
    protected void doEvict(K key) {
        _mapCache.remove(key);
    }

    @Override
    protected void doClear() {
        _mapCache.clear();
    }

    @Override
    protected long doSize() {
        Integer size = _mapCache.size();
        return size != null ? size : 0;
    }

    @Override
    public void put(K key, V value, @Nullable Duration ttl) {
        long ttlMillis = ttl != null ? ttl.toMillis() : _defaultTtlMillis;
        putWithTtl(key, value, ttlMillis);
    }

    private void putWithTtl(K key, V value, long ttlMillis) {
        if (ttlMillis > 0) {
            _mapCache.fastPut(key, value, ttlMillis, TimeUnit.MILLISECONDS);
        } else {
            _mapCache.put(key, value);
        }
    }
}