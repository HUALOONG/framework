package cn.jowen.framework.cache.api;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.LongAdder;

/**
 * 缓存统计。累计命中/未命中计数，提供命中率（可观测出口的标准来源）。
 * 线程安全，使用 {@link LongAdder} 降低高并发竞争。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class CacheStats {

    private final LongAdder hits = new LongAdder();
    private final LongAdder misses = new LongAdder();

    /**
     * 记录一次命中。
     */
    public void recordHit() {
        hits.increment();
    }

    /**
     * 记录一次未命中。
     */
    public void recordMiss() {
        misses.increment();
    }

    /**
     * @return 累计命中数
     */
    public long hits() {
        return hits.sum();
    }

    /**
     * @return 累计未命中数
     */
    public long misses() {
        return misses.sum();
    }

    /**
     * @return 总请求数（命中 + 未命中）
     */
    public long total() {
        return hits.sum() + misses.sum();
    }

    /**
     * 命中率 = 命中 / 总请求，范围 [0,1]。
     *
     * @return 命中率；无请求时返回 1.0（视为全命中，避免除零噪声）
     */
    public double hitRate() {
        long total = total();
        return total == 0 ? 1.0 : (double) hits.sum() / total;
    }

    /**
     * @return 未命中率，范围 [0,1]
     */
    public double missRate() {
        return 1.0 - hitRate();
    }

    @Override
    public String toString() {
        return String.format("CacheStats{hits=%d, misses=%d, hitRate=%.2f}",
                hits.sum(), misses.sum(), hitRate());
    }
}
