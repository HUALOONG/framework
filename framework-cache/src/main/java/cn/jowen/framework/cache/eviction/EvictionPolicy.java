package cn.jowen.framework.cache.eviction;

import org.jspecify.annotations.NullMarked;

/**
 * 缓存淘汰策略接口。各实现定义不同的淘汰算法（LRU/LFU/TTL 等），与底层缓存实现解耦。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public interface EvictionPolicy {

    EvictionPolicy LRU = new LruEvictionPolicy();

    String getType();

    boolean shouldEvict(EvictionContext context);
}
