package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.cache.CacheManager;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 缓存装配集成测试：验证空配置下 {@link CacheManager} 自动装配，且命中率指标可观测。
 */
class CacheAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class))
            .withBean(SimpleMeterRegistry.class);

    @Test
    void cacheManagerRegistered() {
        runner.run(context -> assertThat(context).hasSingleBean(CacheManager.class));
    }

    @Test
    void hitRateExposedAsMetric() {
        runner.run(context -> {
            CacheManager manager = context.getBean(CacheManager.class);
            var cache = manager.getCache("user");
            cache.put("a", "1");
            cache.get("a"); // hit
            cache.get("z"); // miss

            SimpleMeterRegistry registry = context.getBean(SimpleMeterRegistry.class);
            // 手动将 MeterBinder 绑定到 registry（模拟 Spring Boot MetricsAutoConfiguration 行为）
            io.micrometer.core.instrument.binder.MeterBinder binder = context.getBean(io.micrometer.core.instrument.binder.MeterBinder.class);
            binder.bindTo(registry);

            io.micrometer.core.instrument.Gauge gauge = registry.find("framework.cache.hitRate")
                    .tag("cache", "user").gauge();
            assertThat(gauge).isNotNull();
            assertThat(gauge.value()).isCloseTo(0.5, org.assertj.core.data.Offset.offset(0.001));
        });
    }

    @Test
    void cacheDisabledByProperty() {
        runner.withPropertyValues("framework.cache.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(CacheManager.class));
    }
}
