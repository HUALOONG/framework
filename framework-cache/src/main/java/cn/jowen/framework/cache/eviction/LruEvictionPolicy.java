package cn.jowen.framework.cache.eviction;

import org.jspecify.annotations.NullMarked;

/**
 * LRU（最近最少使用）淘汰策略。淘汰访问时间最久的条目。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LruEvictionPolicy implements EvictionPolicy {

    @Override
    public String getType() {
        return "lru";
    }

    @Override
    public boolean shouldEvict(EvictionContext context) {
        // LRU 由外部容器（如 Caffeine）驱动，此处仅标记策略类型
        return false;
    }
}
