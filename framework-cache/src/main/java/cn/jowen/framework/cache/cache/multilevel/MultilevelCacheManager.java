package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多级缓存管理器。按名称注册和获取 {@link MultilevelCache} 实例，
 * 集中管理本地层与远程层的组合缓存。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class MultilevelCacheManager implements CacheManager {

    private final Map<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    /**
     * 跨节点同步广播器工厂。设置后，{@link #registerCache} 会将每个多级缓存包装为
     * {@link SyncPublishingCache}，在本地写/失效时广播变更，实现跨节点一致性。
     */
    private @Nullable CacheSyncBroadcasterFactory broadcasterFactory;

    /**
     * 设置跨节点同步广播器工厂（可选）。
     *
     * @param factory 广播器工厂，可为 {@code null} 关闭同步
     */
    public void setCacheSyncBroadcasterFactory(@Nullable CacheSyncBroadcasterFactory factory) {
        this.broadcasterFactory = factory;
    }

    /**
     * 注册多级缓存。
     *
     * @param name       缓存名，不可为 {@code null}
     * @param local      本地缓存，不可为 {@code null}
     * @param remote     远程缓存，不可为 {@code null}
     * @param nullValueCache 是否缓存空值（防穿透）
     * @param nullTtl    空值 TTL；{@code null} 或零表示不缓存空值
     * @param mutexLock  是否启用互斥锁（防击穿）
     */
    public void registerCache(String name, Cache<?, ?> local, Cache<?, ?> remote,
                              boolean nullValueCache, @Nullable Duration nullTtl, boolean mutexLock) {
        @SuppressWarnings("unchecked")
        Cache<Object, Object> multilevel = new MultilevelCache<>(
                name,
                (Cache<Object, Object>) local,
                (Cache<Object, Object>) remote,
                nullValueCache,
                nullTtl != null ? nullTtl : Duration.ZERO,
                mutexLock
        );
        Cache<Object, Object> toRegister = multilevel;
        if (broadcasterFactory != null) {
            toRegister = new SyncPublishingCache(multilevel, broadcasterFactory.create(name, multilevel));
        }
        caches.put(name, toRegister);
    }

    /**
     * 构建但不注册多级缓存实例，供需要附加装饰（如跨节点同步 {@link SyncPublishingCache}）的调用方使用。
     *
     * @param name           缓存名，不可为 {@code null}
     * @param local          本地缓存，不可为 {@code null}
     * @param remote        远程缓存，不可为 {@code null}
     * @param nullValueCache 是否缓存空值（防穿透）
     * @param nullTtl        空值 TTL；{@code null} 或零表示不缓存空值
     * @param mutexLock      是否启用互斥锁（防击穿）
     * @return 多级缓存实例（未注册）
     */
    public Cache<Object, Object> buildMultilevelCache(String name, Cache<?, ?> local, Cache<?, ?> remote,
                                                      boolean nullValueCache, @Nullable Duration nullTtl, boolean mutexLock) {
        return new MultilevelCache<>(
                name,
                (Cache<Object, Object>) local,
                (Cache<Object, Object>) remote,
                nullValueCache,
                nullTtl != null ? nullTtl : Duration.ZERO,
                mutexLock
        );
    }

    /**
     * 注册多级缓存（默认配置）。
     *
     * @param name   缓存名，不可为 {@code null}
     * @param local  本地缓存，不可为 {@code null}
     * @param remote 远程缓存，不可为 {@code null}
     */
    public void registerCache(String name, Cache<?, ?> local, Cache<?, ?> remote) {
        registerCache(name, local, remote, false, null, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> Cache<K, V> getCache(String name) {
        Cache<K, V> cache = (Cache<K, V>) caches.get(name);
        if (cache == null) {
            throw new IllegalArgumentException("Cache not found: " + name);
        }
        return cache;
    }

    @Override
    public @Nullable CacheStats getStats(String name) {
        Cache<?, ?> cache = caches.get(name);
        return cache == null ? null : cache.stats();
    }

    @Override
    public Iterable<String> cacheNames() {
        return caches.keySet();
    }

    @Override
    public void register(Cache<?, ?> cache) {
        caches.put(cache.name(), cache);
    }

    @Override
    public Map<String, Cache<?, ?>> asMap() {
        return caches;
    }
}
