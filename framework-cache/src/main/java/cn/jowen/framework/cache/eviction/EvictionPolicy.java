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

    /**
     * @return 策略类型名称
     */
    String getType();

    /**
     * 判断是否应对指定条目执行淘汰。
     *
     * @param context 淘汰上下文
     * @return {@code true} 表示应淘汰
     */
    boolean shouldEvict(EvictionContext context);

    /**
     * 淘汰策略上下文。
     */
    @NullMarked
    interface EvictionContext {
        /**
         * @return 键
         */
        Object key();

        /**
         * @return 值
         */
        Object value();

        /**
         * @return 访问次数
         */
        long accessCount();

        /**
         * @return 写入时间戳（毫秒）
         */
        long writeTimeMillis();
    }
}
