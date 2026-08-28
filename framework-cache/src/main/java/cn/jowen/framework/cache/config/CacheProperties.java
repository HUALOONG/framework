package cn.jowen.framework.cache.config;

import org.jspecify.annotations.NullMarked;

/**
 * 缓存配置属性（纯 POJO）。绑定前缀 {@code framework.cache.*} 由 boot-autoconfigure 的
 * {@code BootCacheProperties} 完成。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class CacheProperties {

    /**
     * 是否启用框架缓存装配，缺省开启。
     */
    private boolean enabled = true;

    /**
     * 是否将缓存命中率暴露为 Micrometer 指标（需 actuator/micrometer 在 classpath），缺省开启。
     */
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
