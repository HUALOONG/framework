package cn.jowen.framework.cache.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Optional;

/**
 * 缓存操作接口。所有缓存实现（本地/Caffeine/Redisson/多级）均实现此接口。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface Cache<K, V> {

    /**
     * @return 缓存名称（同一 {@link CacheManager} 内唯一），不可为 {@code null}
     */
    String name();

    /**
     * 读取缓存。
     *
     * @param key 键，不可为 {@code null}
     * @return 命中值；未命中或已过期返回 {@code null}
     */
    @Nullable V get(K key);

    /**
     * 读取缓存，未命中返回 {@link Optional#empty()}。
     *
     * @param key 键，不可为 {@code null}
     * @return 命中值包装，不可为 {@code null}
     */
    default Optional<V> getOptional(K key) {
        return Optional.ofNullable(get(key));
    }

    /**
     * 写入缓存（覆盖已存在值）。
     *
     * @param key   键，不可为 {@code null}
     * @param value 值，不可为 {@code null}
     */
    void put(K key, V value);

    /**
     * 写入缓存并指定 TTL。
     *
     * @param key   键，不可为 {@code null}
     * @param value 值，不可为 {@code null}
     * @param ttl   过期时间；{@code null} 或零表示使用默认 TTL
     */
    default void put(K key, V value, @Nullable Duration ttl) {
        put(key, value);
    }

    /**
     * 仅当不存在时写入。
     *
     * @param key   键，不可为 {@code null}
     * @param value 值，不可为 {@code null}
     * @return 写入成功返回 {@code true}
     */
    boolean putIfAbsent(K key, V value);

    /**
     * 移除指定键。
     *
     * @param key 键，不可为 {@code null}
     */
    void evict(K key);

    /**
     * 清空全部条目。
     */
    void clear();

    /**
     * @return 当前条目数
     */
    long size();

    /**
     * @return 该缓存的统计信息（命中率等），不可为 {@code null}
     */
    CacheStats stats();
}
