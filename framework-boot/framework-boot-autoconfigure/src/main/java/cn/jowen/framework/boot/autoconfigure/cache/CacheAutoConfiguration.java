package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.cache.CacheManager;
import cn.jowen.framework.cache.CacheStats;
import cn.jowen.framework.cache.DefaultCacheManager;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 缓存装配。向容器提供 {@link CacheManager}（默认 {@link DefaultCacheManager}）。
 *
 * <p>当 classpath 存在 Micrometer 且开启 {@code framework.cache.metrics} 时，自动将各缓存的命中率/命中数/未命中数
 * 注册为指标（{@code framework.cache.hitRate} 等），满足"多级缓存命中率可观测"的出口标准。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration(after = BootAutoConfiguration.class)
@ConditionalOnClass(name = "cn.jowen.framework.cache.CacheManager")
@ConditionalOnProperty(prefix = "framework.cache", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(CacheProperties.class)
public class CacheAutoConfiguration {

    /**
     * 缓存管理器。用户可通过它按需创建本地/组合缓存。
     *
     * @return 缓存管理器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new DefaultCacheManager();
    }

    /**
     * 缓存指标绑定器（可选）。仅当 Micrometer 在 classpath 且开启指标时生效。
     *
     * @param manager  缓存管理器，不可为 {@code null}
     * @param properties 属性，不可为 {@code null}
     * @return 指标绑定器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnClass(name = "io.micrometer.core.instrument.MeterRegistry")
    @ConditionalOnProperty(prefix = "framework.cache", name = "metrics", matchIfMissing = true)
    public MeterBinder cacheMetrics(CacheManager manager, CacheProperties properties) {
        return registry -> {
            for (String name : manager.cacheNames()) {
                bindCache(registry, manager, name);
            }
        };
    }

    private void bindCache(MeterRegistry registry, CacheManager manager, String cacheName) {
        CacheStats stats = manager.getStats(cacheName);
        if (stats == null) {
            return;
        }
        registry.gauge("framework.cache.hits", java.util.Collections.singletonList(
                io.micrometer.core.instrument.Tag.of("cache", cacheName)), stats, CacheStats::hits);
        registry.gauge("framework.cache.misses", java.util.Collections.singletonList(
                io.micrometer.core.instrument.Tag.of("cache", cacheName)), stats, CacheStats::misses);
        registry.gauge("framework.cache.hitRate", java.util.Collections.singletonList(
                io.micrometer.core.instrument.Tag.of("cache", cacheName)), stats, CacheStats::hitRate);
    }
}
