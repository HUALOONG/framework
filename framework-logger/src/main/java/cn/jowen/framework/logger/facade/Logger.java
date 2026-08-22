package cn.jowen.framework.logger.facade;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 日志门面接口。业务代码仅依赖本接口，不感知底层 Logback/Log4j2 实现。
 *
 * <p>所有带 {@code {} } 占位符的方法在最终输出前经脱敏器处理，保证敏感信息不落盘。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface Logger {

    void trace(String msg, Object... args);

    void debug(String msg, Object... args);

    void info(String msg, Object... args);

    void warn(String msg, Object... args);

    void error(String msg, Object... args);

    void error(String msg, Throwable t, Object... args);

    /**
     * 判断是否启用指定级别（避免昂贵拼接）。
     *
     * @param level 级别，不可为 {@code null}
     * @return 启用返回 {@code true}
     */
    boolean isEnabled(LogLevel level);
}
