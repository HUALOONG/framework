package cn.jowen.framework.cache.event;

import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * 缓存未命中事件。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class CacheMissEvent extends CacheEvent {

    public CacheMissEvent(String cacheName, String key, Instant timestamp) {
        super(cacheName, key, timestamp, CacheEventType.MISS);
    }
}
