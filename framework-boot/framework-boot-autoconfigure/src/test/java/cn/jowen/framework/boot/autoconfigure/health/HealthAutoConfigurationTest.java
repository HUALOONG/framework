package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 健康检查装配集成测试：验证健康指示器注册并正确汇总框架状态。
 */
class HealthAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class));

    @Test
    void healthIndicatorRegistered() {
        runner.run(context -> assertThat(context).hasSingleBean(BootHealthIndicator.class));
    }

    @Test
    void healthReportsUpWithFrameworkDetails() {
        runner.run(context -> {
            BootHealthIndicator indicator = context.getBean(BootHealthIndicator.class);
            Health health = indicator.health();
            assertThat(health.getStatus()).isEqualTo(Status.UP);
            assertThat(health.getDetails()).containsKey("cache.caches");
            assertThat(health.getDetails()).containsKey("plugin.plugins");
            assertThat(health.getDetails()).containsKey("i18n.available");
        });
    }
}
