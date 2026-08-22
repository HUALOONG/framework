package cn.jowen.framework.logger.config;

import org.jspecify.annotations.NullMarked;

/**
 * 日志模块配置属性，对应 {@code framework.logger.*} 与 {@code framework.desensitize.enabled}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class LoggerProperties {

    /** 日志门面总开关。 */
    private boolean enabled = true;

    /** 异步日志（虚拟线程消费）。 */
    private boolean asyncEnabled = true;

    /** 默认级别。 */
    private String level = "info";

    /** 输出格式：text / json。 */
    private String format = "text";

    /** 脱敏全局开关（与 core 统一，日志/结果集/JSON 共用）。 */
    private boolean desensitizeEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAsyncEnabled() {
        return asyncEnabled;
    }

    public void setAsyncEnabled(boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isDesensitizeEnabled() {
        return desensitizeEnabled;
    }

    public void setDesensitizeEnabled(boolean desensitizeEnabled) {
        this.desensitizeEnabled = desensitizeEnabled;
    }
}
