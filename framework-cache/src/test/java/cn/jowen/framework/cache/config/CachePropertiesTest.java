package cn.jowen.framework.cache.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheProperties} 配置 POJO 单元测试。
 *
 * <p>覆盖：默认值（enabled / metrics 均为 true），以及 setter 正确更新字段、getter 正确回读。
 */
class CachePropertiesTest {

    @Test
    void defaults_enabledAndMetricsTrue() {
        CacheProperties properties = new CacheProperties();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isMetrics()).isTrue();
    }

    @Test
    void setters_updateValues() {
        CacheProperties properties = new CacheProperties();

        properties.setEnabled(false);
        properties.setMetrics(false);
        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isMetrics()).isFalse();

        properties.setEnabled(true);
        properties.setMetrics(true);
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isMetrics()).isTrue();
    }
}
