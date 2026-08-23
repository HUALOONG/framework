package cn.jowen.framework.logger.mask;

import cn.jowen.framework.core.desensitize.Desensitizer;
import cn.jowen.framework.logger.config.LoggerProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 日志脱敏入口。按 {@link LoggerProperties} 的全局开关，委托 core 脱敏器对消息与参数做脱敏。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class LogMasker {
    /**
     * 日志模块配置属性，不可为 {@code null}
     */
    private final LoggerProperties properties;
    /**
     * 脱敏器，不可为 {@code null}
     */
    private final Desensitizer desensitizer;

    /**
     * 日志脱敏组件，委托 core 脱敏器按配置开关工作。
     *
     * @param properties 日志属性，不可为 {@code null}
     */
    public LogMasker(LoggerProperties properties) {
        this(properties, Desensitizer.getInstance());
    }

    /**
     * 日志脱敏组件，委托 core 脱敏器按配置开关工作。
     *
     * @param properties 日志属性，不可为 {@code null}
     * @param desensitizer 脱敏器，不可为 {@code null}
     */
    public LogMasker(LoggerProperties properties, Desensitizer desensitizer) {
        this.properties = properties;
        this.desensitizer = desensitizer;
    }

    /**
     * 脱敏日志消息。开关关闭时原样返回。
     *
     * @param message 原始消息，可为 {@code null}
     * @return 脱敏后消息
     */
    public @Nullable String maskMessage(@Nullable String message) {
        if (message == null || !properties.isDesensitizeEnabled()) {
            return message;
        }
        return desensitizer.mask(message);
    }

    /**
     * 脱敏参数数组。开关关闭时原样返回。
     *
     * @param args 参数，可为 {@code null}
     * @return 脱敏后参数（新数组）
     */
    public Object @Nullable [] maskArgs(Object @Nullable [] args) {
        if (args == null || !properties.isDesensitizeEnabled()) {
            return args;
        }
        Object[] result = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            Object a = args[i];
            result[i] = a instanceof String s ? Objects.requireNonNull(desensitizer.mask(s)) : a;
        }
        return result;
    }
}
