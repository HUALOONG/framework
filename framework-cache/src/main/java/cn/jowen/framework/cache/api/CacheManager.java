package cn.jowen.framework.cache.api;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * 缓存管理器接口。按名称获取或创建缓存，并集中暴露各缓存的统计（可观测入口）。
 *
 * @author 王飞
 * @since 2026-08-24
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
    CacheStats getStats(String name);

    /**
     * 返回所有缓存名。
     */
    Iterable<String> cacheNames();

    /**
     * 注册外部构建的缓存（如组合缓存），便于统一管理。
     */
    void register(Cache<?, ?> cache);

    /**
     * 返回底层 Map 视图（供兼容场景使用）。
     */
    Map<String, Cache<?, ?>> asMap();
}
