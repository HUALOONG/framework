package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 通知配置载体（轻量 POJO，非 Spring {@code @ConfigurationProperties}）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class NotificationProperties {

    /**
     * 默认发送方标识。
     */
    private String defaultFrom = "noreply@jowen.cn";

    /**
     * 是否启用异步发送。
     */
    private boolean asyncEnabled = true;

    public NotificationProperties() {
    }

    public String getDefaultFrom() {
        return defaultFrom;
    }

    public void setDefaultFrom(String defaultFrom) {
        this.defaultFrom = defaultFrom;
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }
}
