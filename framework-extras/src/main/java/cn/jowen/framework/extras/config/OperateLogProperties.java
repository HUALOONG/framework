package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 操作日志配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class OperateLogProperties {

    /**
     * 是否启用操作日志。
     */
    private boolean enabled = true;

    /**
     * 默认是否异步分发。
     */
    private boolean async = true;

    public OperateLogProperties() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAsync() {
        return async;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }
}
