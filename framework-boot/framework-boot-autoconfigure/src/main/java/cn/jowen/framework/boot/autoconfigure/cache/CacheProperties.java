package cn.jowen.framework.boot.autoconfigure.cache;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 缓存配置属性。绑定 {@code framework.cache.*}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.cache")
public class CacheProperties {

    /** 是否启用框架缓存装配，缺省开启。 */
    private boolean enabled = true;

    /** 是否将缓存命中率暴露为 Micrometer 指标（需 actuator/micrometer 在 classpath），缺省开启。 */
    private boolean metrics = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMetrics() {
        return metrics;
    }

    public void setMetrics(boolean metrics) {
        this.metrics = metrics;
    }
}
