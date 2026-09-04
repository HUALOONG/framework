package cn.jowen.framework.cache.cache.multilevel;

import org.jspecify.annotations.Nullable;

/**
 * 跨节点缓存变更广播（发布侧）。与接收侧 {@link CacheSyncListener} 解耦，
 * 便于在 {@link MultilevelCacheManager} 中以装饰器方式接入，且不向缓存模块泄露 Redis 实现细节。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
public interface CacheSyncBroadcaster {

    /**
     * 广播一次写操作，供其他节点回填本地缓存。
     *
     * @param key   键，不可为 {@code null}
     * @param value 值，可为 {@code null}
     */
    void publishPut(String key, @Nullable Object value);

    /**
     * 广播一次失效操作，供其他节点剔除本地缓存。
     *
     * @param key 键，不可为 {@code null}
     */
    void publishEvict(String key);
}
