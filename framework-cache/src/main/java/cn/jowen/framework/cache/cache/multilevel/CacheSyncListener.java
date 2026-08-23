package cn.jowen.framework.cache.cache.multilevel;

import cn.jowen.framework.cache.api.Cache;
import org.jspecify.annotations.NullMarked;

/**
 * 缓存同步监听器接口。用于跨节点缓存同步通知（如 Redis Pub/Sub、MQ 等）。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public interface CacheSyncListener {

    /**
     * 缓存写入事件通知。
     *
     * @param key     缓存键，不可为 {@code null}
     * @param value   缓存值，可能为 {@code null}
     * @param cacheName 缓存名称，不可为 {@code null}
     */
    void onCachePut(String key, Object value, String cacheName);

    /**
     * 缓存清除事件通知。
     *
     * @param key       缓存键，不可为 {@code null}
     * @param cacheName 缓存名称，不可为 {@code null}
     */
    void onCacheEvict(String key, String cacheName);
}
