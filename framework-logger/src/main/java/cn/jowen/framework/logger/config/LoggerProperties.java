package cn.jowen.framework.logger.config;

import org.jspecify.annotations.NullMarked;

/**
 * 日志模块配置属性，对应 {@code framework.logger.*} 与 {@code framework.desensitize.enabled}。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class LoggerProperties {

    /**
     * 日志门面总开关。
     */
    private boolean enabled = true;

    /**
     * 异步日志（虚拟线程消费）。
     */
    private boolean asyncEnabled = true;

    /**
     * 默认级别。
     */
    private String level = "info";

    /**
     * 输出格式：text / json。
     */
    private String format = "text";

    /**
     * 脱敏全局开关（与 core 统一，日志/结果集/JSON 共用）。
     */
    private boolean desensitizeEnabled = true;

    /**
     * 日志门面总开关。
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 日志门面总开关。
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 异步日志（虚拟线程消费）。
     */
    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    /**
     * 异步日志（虚拟线程消费）。
     */
    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    /**
     * 默认级别。
     */
    public String getLevel() {
        return level;
    }

    /**
     * 默认级别。
     */
    public void setLevel(String level) {
        this.level = level;
    }

    /**
     * 输出格式：text / json。
     */
    public String getFormat() {
        return format;
    }

    /**
     * 输出格式：text / json。
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * 脱敏全局开关（与 core 统一，日志/结果集/JSON 共用）。
     */
    public boolean isDesensitizeEnabled() {
        return desensitizeEnabled;
    }

    /**
     * 脱敏全局开关（与 core 统一，日志/结果集/JSON 共用）。
     */
    public void setDesensitizeEnabled(boolean desensitizeEnabled) {
        this.desensitizeEnabled = desensitizeEnabled;
    }
}
