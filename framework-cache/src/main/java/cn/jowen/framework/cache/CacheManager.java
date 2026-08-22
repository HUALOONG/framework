package cn.jowen.framework.cache;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 缓存管理器。按名称获取或创建缓存，并集中暴露各缓存的统计（可观测入口）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface CacheManager {

    /**
     * 获取或创建命名缓存。
     *
     * @param name 缓存名，不可为 {@code null}
     * @param <K>  键类型
     * @param <V>  值类型
     * @return 缓存实例，不可为 {@code null}
     */
    <K, V> Cache<K, V> getCache(String name);

    /**
     * 获取已存在缓存的统计；不存在返回 {@code null}。
     *
     * @param name 缓存名，不可为 {@code null}
     * @return 统计信息或 {@code null}
     */
    @Nullable CacheStats getStats(String name);

    /** 返回所有缓存名。 */
    Iterable<String> cacheNames();
}
