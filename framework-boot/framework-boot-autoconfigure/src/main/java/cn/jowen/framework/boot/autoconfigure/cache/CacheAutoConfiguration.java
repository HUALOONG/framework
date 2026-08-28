package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
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
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@EnableConfigurationProperties(BootCacheProperties.class)
@ConditionalOnProperty(prefix = "framework.cache", name = "enabled", matchIfMissing = true)
public class CacheAutoConfiguration {

    /**
     * 缓存管理器。用户可通过它按需创建本地/组合缓存。
     *
     * @return 缓存管理器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager(BootCacheProperties properties) {
        return new DefaultCacheManager();
    }

    /**
     * 缓存指标上报器（可选）。仅当 Micrometer 在 classpath 且开启指标时生效，
     * 由 {@link CacheStatsReporter} 将各缓存命中率注册为 gauge。
     *
     * @param manager 缓存管理器，不可为 {@code null}
     * @return 指标上报器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnClass(name = "MeterRegistry")
    @ConditionalOnProperty(prefix = "framework.cache", name = "metrics", matchIfMissing = true)
    public MeterBinder cacheMetrics(CacheManager manager) {
        return new CacheStatsReporter(manager);
    }
}
