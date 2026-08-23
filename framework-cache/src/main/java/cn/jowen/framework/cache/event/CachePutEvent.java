package cn.jowen.framework.cache.event;

import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * 缓存写入事件。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class CachePutEvent extends CacheEvent {

    private final long ttlMillis;

    public CachePutEvent(String cacheName, String key, Instant timestamp, long ttlMillis) {
        super(cacheName, key, timestamp, CacheEventType.PUT);
        this.ttlMillis = ttlMillis;
    }

    public long ttlMillis() {
        return ttlMillis;
    }
}
