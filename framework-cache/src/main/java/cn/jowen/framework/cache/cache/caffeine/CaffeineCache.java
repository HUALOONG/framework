package cn.jowen.framework.cache.cache.caffeine;

import cn.jowen.framework.cache.AbstractCache;
import cn.jowen.framework.cache.api.CacheConfiguration;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 基于 Caffeine 库的一级本地缓存实现。支持容量上限、写入过期、访问过期与 LRU 淘汰。
 *
 * <p>注意：Caffeine 的 TTL 策略在缓存创建时统一设定，不支持逐键独立 TTL；
 * {@link #put(Object, Object, Duration)} 的自定义 TTL 会回退到创建时的默认过期策略。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class CaffeineCache<K, V> extends AbstractCache<K, V> {

    private final com.github.benmanes.caffeine.cache.Cache<K, V> cache;

    public CaffeineCache(String name, CacheConfiguration config) {
        super(name);
        this.cache = buildCache(config);
    }

    /**
     * 创建 Caffeine 缓存实例。
     *
     * @param name   缓存名称，不可为 {@code null}
     * @param config 缓存配置，不可为 {@code null}
     * @return 缓存实例，不可为 {@code null}
     */
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
    protected @Nullable V doGet(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    protected void doPut(K key, V value) {
        cache.put(key, value);
    }

    @Override
    protected void doEvict(K key) {
        cache.invalidate(key);
    }

    @Override
    protected void doClear() {
        cache.invalidateAll();
    }

    @Override
    protected long doSize() {
        return cache.estimatedSize();
    }
}