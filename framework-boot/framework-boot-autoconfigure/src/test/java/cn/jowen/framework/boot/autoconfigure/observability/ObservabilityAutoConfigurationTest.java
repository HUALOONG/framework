package cn.jowen.framework.boot.autoconfigure.observability;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ObservabilityAutoConfiguration} 装配测试：集中总闸与属性绑定。
 */
class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ObservabilityAutoConfiguration.class));

    @Test
    void defaultContext_bindsProperties() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(BootObservabilityProperties.class);
            BootObservabilityProperties props = ctx.getBean(BootObservabilityProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getMetricsPrefix()).isEqualTo("framework");
        });
    }

    @Test
    void customPrefix_bound() {
        context.withPropertyValues("framework.observability.metrics-prefix=app")
                .run(ctx -> assertThat(ctx.getBean(BootObservabilityProperties.class).getMetricsPrefix())
                        .isEqualTo("app"));
    }

    @Test
    void disabled_noPropertiesBean() {
        context.withPropertyValues("framework.observability.enabled=false")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(BootObservabilityProperties.class));
    }
}