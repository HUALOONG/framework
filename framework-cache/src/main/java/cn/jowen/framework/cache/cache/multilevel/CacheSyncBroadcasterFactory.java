package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import org.jspecify.annotations.NullMarked;

/**
 * 为每个多级缓存创建跨节点同步广播器的工厂。由 {@link MultilevelCacheManager} 在注册缓存时调用，
 * 将本地缓存实例与 Redis 主题名绑定，生产出 {@link CacheSyncBroadcaster}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface CacheSyncBroadcasterFactory {

    /**
     * 为指定多级缓存创建广播器。
     *
     * @param cacheName  缓存名，不可为 {@code null}
     * @param localCache 本地多级缓存（远端事件将直接写入此处），不可为 {@code null}
     * @return 广播器，不可为 {@code null}
     */
    CacheSyncBroadcaster create(String cacheName, Cache<Object, Object> localCache);
}
