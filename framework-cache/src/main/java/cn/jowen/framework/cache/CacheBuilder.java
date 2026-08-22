package cn.jowen.framework.cache;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 缓存构建器。流式构造 {@link LocalCache} 或 {@link LayeredCache}。
 *
 * <p>示例：
 * <pre>{@code
 * Cache<String, User> c = CacheBuilder.local("user", 30_000L).build();
 * Cache<String, User> layered = CacheBuilder.layered("user", local, redis).nullTtl(5_000L).build();
 * }</pre>
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class CacheBuilder {

    private CacheBuilder() {
    }

    /**
     * 构造本地缓存构建器。
     *
     * @param name      缓存名，不可为 {@code null}
     * @param ttlMillis TTL（毫秒），{@code <=0} 表示不过期
     * @param <K>       键类型
     * @param <V>       值类型
     * @return 构建器，不可为 {@code null}
     */
    public static <K, V> LocalCacheBuilder<K, V> local(String name, long ttlMillis) {
        return new LocalCacheBuilder<>(name, ttlMillis);
    }

    /**
     * 构造组合缓存构建器。
     *
     * @param name   缓存名，不可为 {@code null}
     * @param local  本地层，不可为 {@code null}
     * @param backup 后备层，不可为 {@code null}
     * @param <K>    键类型
     * @param <V>    值类型
     * @return 构建器，不可为 {@code null}
     */
    public static <K, V> LayeredCacheBuilder<K, V> layered(String name,
                                                           Cache<K, V> local,
                                                           Cache<K, V> backup) {
        return new LayeredCacheBuilder<>(name, local, backup);
    }

    /** 本地缓存构建器。 */
    @NullMarked
    public static final class LocalCacheBuilder<K, V> {
        private final String name;
        private final long ttlMillis;

        private LocalCacheBuilder(String name, long ttlMillis) {
            this.name = name;
            this.ttlMillis = ttlMillis;
        }

        /** @return 本地缓存实例，不可为 {@code null} */
        public LocalCache<K, V> build() {
            return new LocalCache<>(name, ttlMillis);
        }
    }

    /** 组合缓存构建器。 */
    @NullMarked
    public static final class LayeredCacheBuilder<K, V> {
        private final String name;
        private final Cache<K, V> local;
        private final Cache<K, V> backup;
        private long nullTtlMillis = 0L;

        private LayeredCacheBuilder(String name, Cache<K, V> local, Cache<K, V> backup) {
            this.name = name;
            this.local = local;
            this.backup = backup;
        }

        /** @param nullTtlMillis 空值占位 TTL（毫秒），{@code <=0} 不缓存空值 @return this */
        public LayeredCacheBuilder<K, V> nullTtl(long nullTtlMillis) {
            this.nullTtlMillis = nullTtlMillis;
            return this;
        }

        /** @return 组合缓存实例，不可为 {@code null} */
        public LayeredCache<K, V> build() {
            return new LayeredCache<>(name, local, backup, nullTtlMillis);
        }
    }
}
