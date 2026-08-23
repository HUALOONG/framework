package cn.jowen.framework.cache.event;

import org.jspecify.annotations.NullMarked;

/**
 * 缓存事件类型枚举。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public enum CacheEventType {

    /**
     * 缓存命中
     */
    HIT,
    /**
     * 缓存未命中
     */
    MISS,
    /**
     * 缓存写入
     */
    PUT,
    /**
     * 缓存清除
     */
    EVICT,
    /**
     * 缓存全部清除
     */
    CLEAR
}
