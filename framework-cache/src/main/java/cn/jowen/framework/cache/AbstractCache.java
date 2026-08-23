package cn.jowen.framework.cache;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 带统计能力的 {@link Cache} 骨架。子类只需实现 {@code doGet/doPut/doEvict/doClear/doSize}，
 * 命中率统计由本类统一记录，保证可观测口径一致。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public abstract class AbstractCache<K, V> implements Cache<K, V> {

    private final String name;
    private final CacheStats stats = new CacheStats();

    protected AbstractCache(String name) {
        this.name = name;
    }

    @Override
    public final String name() {
        return name;
    }

    @Override
    public final @Nullable V get(K key) {
        V value = doGet(key);
        if (value == null) {
            stats.recordMiss();
        } else {
            stats.recordHit();
        }
        return value;
    }

    @Override
    public final Optional<V> getOptional(K key) {
        return Cache.super.getOptional(key);
    }

    @Override
    public final void put(K key, V value) {
        doPut(key, value);
    }

    @Override
    public final boolean putIfAbsent(K key, V value) {
        if (doGet(key) == null) {
            doPut(key, value);
            return true;
        }
        return false;
    }

    @Override
    public final void evict(K key) {
        doEvict(key);
    }

    @Override
    public final void clear() {
        doClear();
    }

    @Override
    public final CacheStats stats() {
        return stats;
    }

    @Override
    public final long size() {
        return doSize();
    }

    /**
     * 实际读取值（不含统计），未命中返回 {@code null}。
     */
    protected abstract @Nullable V doGet(K key);

    /**
     * 实际写入。
     */
    protected abstract void doPut(K key, V value);

    /**
     * 实际移除。
     */
    protected abstract void doEvict(K key);

    /**
     * 实际清空。
     */
    protected abstract void doClear();

    /**
     * @return 当前条目数
     */
    protected abstract long doSize();
}
