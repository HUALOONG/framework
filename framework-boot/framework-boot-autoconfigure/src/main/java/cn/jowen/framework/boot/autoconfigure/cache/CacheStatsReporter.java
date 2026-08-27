package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.CacheStats;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collections;

/**
 * 缓存统计上报器：将各命名缓存的命中数/未命中数/命中率注册为 Micrometer gauge。
 *
 * <p>与 {@link CacheAutoConfiguration} 配合使用，满足"多级缓存命中率可观测"出口标准。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class CacheStatsReporter implements MeterBinder {

    private final CacheManager cacheManager;

    /**
     * 构造上报器。
     *
     * @param cacheManager 缓存管理器，不可为 {@code null}
     */
    public CacheStatsReporter(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        for (String name : cacheManager.cacheNames()) {
            @Nullable CacheStats stats = cacheManager.getStats(name);
            if (stats == null) {
                continue;
            }
            Iterable<Tag> tags = Collections.singletonList(Tag.of("cache", name));
            registry.gauge("framework.cache.hits", tags, stats, CacheStats::hits);
            registry.gauge("framework.cache.misses", tags, stats, CacheStats::misses);
            registry.gauge("framework.cache.hitRate", tags, stats, CacheStats::hitRate);
        }
    }
}