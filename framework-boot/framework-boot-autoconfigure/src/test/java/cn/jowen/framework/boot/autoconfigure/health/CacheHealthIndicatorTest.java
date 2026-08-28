package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheHealthIndicator} 装配与健康检查测试。
 */
class CacheHealthIndicatorTest {

    private static final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(HealthAutoConfiguration.class));

    @Test
    void health_withCacheManager_reportsCount() {
        runner.withUserConfiguration(CacheManagerConfig.class)
                .run(ctx -> {
                    CacheHealthIndicator indicator = ctx.getBean(CacheHealthIndicator.class);
                    Health health = indicator.health();
                    assertThat(health.getStatus()).isEqualTo(Status.UP);
                    assertThat(health.getDetails().get("caches")).isEqualTo(2);
                });
    }

    @Test
    void health_emptyManager_reportsZero() {
        runner.withUserConfiguration(EmptyManagerConfig.class)
                .run(ctx -> {
                    CacheHealthIndicator indicator = ctx.getBean(CacheHealthIndicator.class);
                    assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
                    assertThat(indicator.health().getDetails().get("caches")).isEqualTo(0);
                });
    }

    @Test
    void noCacheManager_notRegistered() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(CacheHealthIndicator.class));
    }

    @Test
    void healthDisabled_skipsBeans() {
        runner.withPropertyValues("framework.health.enabled=false")
                .withUserConfiguration(CacheManagerConfig.class)
                .run(ctx -> assertThat(ctx).doesNotHaveBean(CacheHealthIndicator.class));
    }

    @Configuration
    static class CacheManagerConfig {
        @Bean
        CacheManager cacheManager() {
            DefaultCacheManager manager = new DefaultCacheManager();
            manager.getCache("userCache");
            manager.getCache("productCache");
            return manager;
        }
    }

    @Configuration
    static class EmptyManagerConfig {
        @Bean
        CacheManager cacheManager() {
            return new DefaultCacheManager();
        }
    }
}