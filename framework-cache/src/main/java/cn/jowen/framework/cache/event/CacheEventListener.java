package cn.jowen.framework.cache.event;

import org.jspecify.annotations.NullMarked;

/**
 * 缓存事件监听器接口。实现此类可监听缓存命中、未命中、写入、清除等事件。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CacheEventListener {

    /**
     * 处理缓存事件。
     *
     * @param event 缓存事件，不可为 {@code null}
     */
    void onEvent(CacheEvent event);
}
