package cn.jowen.framework.cache.event;

import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * 缓存事件基类，所有缓存事件均继承此类。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public abstract class CacheEvent {

    private final String cacheName;
    private final String key;
    private final Instant timestamp;
    private final CacheEventType type;

    protected CacheEvent(String cacheName, String key, Instant timestamp, CacheEventType type) {
        this.cacheName = cacheName;
        this.key = key;
        this.timestamp = timestamp;
        this.type = type;
    }

    public String cacheName() {
        return cacheName;
    }

    public String key() {
        return key;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public CacheEventType type() {
        return type;
    }
}
