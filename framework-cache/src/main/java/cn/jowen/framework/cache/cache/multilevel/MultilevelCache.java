package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.NullValue;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 多级缓存实现（Local + Remote 组合）。读取时先查 L1，未命中再查 L2 并回填；
 * 写入时先写 L2 再写 L1。支持缓存穿透（空值缓存）、缓存击穿（互斥锁）防护。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class MultilevelCache<K, V> implements Cache<K, V> {

    private final String name;
    private final Cache<K, V> localCache;
    private final Cache<K, V> remoteCache;
    private final boolean nullValueCache;
    private final Duration nullTtl;
    private final boolean mutexLock;

    /**
     * 互斥锁对象映射，用于防击穿。
     */
    private final ConcurrentMap<K, Object> mutexLocks = new ConcurrentHashMap<>();

    public MultilevelCache(String name, Cache<K, V> localCache, Cache<K, V> remoteCache) {
        this(name, localCache, remoteCache, false, Duration.ZERO, false);
    }

    public MultilevelCache(String name, Cache<K, V> localCache, Cache<K, V> remoteCache,
                           boolean nullValueCache, Duration nullTtl, boolean mutexLock) {
        this.name = name;
        this.localCache = localCache;
        this.remoteCache = remoteCache;
        this.nullValueCache = nullValueCache;
        this.nullTtl = nullTtl;
        this.mutexLock = mutexLock;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable V get(K key) {
        V localVal = localCache.get(key);
        if (localVal != null) {
            return localVal;
        }
        // 穿透防护：已知空值直接返回
        if (nullValueCache && isNullValue(localVal)) {
            return null;
        }
        // 防击穿：互斥锁
        if (mutexLock) {
            Object lock = mutexLocks.computeIfAbsent(key, k -> new Object());
            synchronized (lock) {
                try {
                    // double-check
                    V fresh = localCache.get(key);
                    if (fresh != null) {
                        return fresh;
                    }
                    if (nullValueCache && isNullValue(fresh)) {
                        return null;
                    }
                    V remoteVal = remoteCache.get(key);
                    if (remoteVal != null) {
                        localCache.put(key, remoteVal);
                        return remoteVal;
                    }
                    // 空值缓存
                    if (nullValueCache) {
                        localCache.put(key, (V) NullValue.INSTANCE);
                        if (nullTtl.compareTo(Duration.ZERO) > 0) {
                            // 本地缓存 TTL 由底层实现管理
                        }
                    }
                    return null;
                } finally {
                    mutexLocks.remove(key, lock);
                }
            }
        }
        // 无锁路径
        V remoteVal = remoteCache.get(key);
        if (remoteVal != null) {
            localCache.put(key, remoteVal);
            return remoteVal;
        }
        if (nullValueCache) {
            localCache.put(key, (V) NullValue.INSTANCE);
        }
        return null;
    }

    @Override
    public void put(K key, V value) {
        remoteCache.put(key, value);
        localCache.put(key, value);
    }

    @Override
    public void put(K key, V value, @Nullable Duration ttl) {
        remoteCache.put(key, value, ttl);
        localCache.put(key, value, ttl);
    }

    @Override
    public boolean putIfAbsent(K key, V value) {
        if (localCache.get(key) != null) {
            return false;
        }
        put(key, value);
        return true;
    }

    @Override
    public void evict(K key) {
        remoteCache.evict(key);
        localCache.evict(key);
    }

    @Override
    public void clear() {
        remoteCache.clear();
        localCache.clear();
    }

    @Override
    public long size() {
        return localCache.size();
    }

    @Override
    public cn.jowen.framework.cache.api.CacheStats stats() {
        // 多级缓存统计合并（以本地为主）
        return localCache.stats();
    }

    /**
     * 回源加载并回填多级缓存。
     *
     * @param key    键，不可为 {@code null}
     * @param loader 回源函数，不可为 {@code null}
     * @return 加载结果，可能为 {@code null}
     */
    public @Nullable V handleCacheMiss(K key, Function<K, @Nullable V> loader) {
        V localVal = localCache.get(key);
        if (localVal != null) {
            return localVal;
        }
        if (nullValueCache && isNullValue(localVal)) {
            return null;
        }
        V loaded = loader.apply(key);
        if (loaded != null) {
            put(key, loaded);
        } else if (nullValueCache) {
            localCache.put(key, (V) NullValue.INSTANCE);
        }
        return loaded;
    }

    /**
     * 判断值是否为空值标记。
     */
    private boolean isNullValue(@Nullable Object value) {
        return value instanceof NullValue;
    }
}
