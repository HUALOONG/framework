package cn.jowen.framework.cache.eviction;

import org.jspecify.annotations.NullMarked;

/**
 * 缓存淘汰上下文，携带待判定条目的键、值与访问信息。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public interface EvictionContext {

    Object key();

    Object value();

    long accessCount();

    long writeTimeMillis();
}
