package cn.jowen.framework.boot.autoconfigure.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BootObservabilityProperties} 属性绑定测试。
 *
 * <p>既有 {@code ObservabilityAutoConfigurationTest} 走条件装配验证 Bean 存在性，
 * 总开关 setter 未触达。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class BootObservabilityPropertiesGapTest {

    @Test
    void defaults_metricsEnabledWithFrameworkPrefix() {
        BootObservabilityProperties properties = new BootObservabilityProperties();

        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getMetricsPrefix()).isEqualTo("framework");
    }

    @Test
    void setters_overrideDefaults() {
        BootObservabilityProperties properties = new BootObservabilityProperties();
        properties.setEnabled(false);
        properties.setMetricsPrefix("jowen");

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getMetricsPrefix()).isEqualTo("jowen");
    }
}
