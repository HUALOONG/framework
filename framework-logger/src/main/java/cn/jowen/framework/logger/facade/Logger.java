package cn.jowen.framework.logger.facade;

import org.jspecify.annotations.NullMarked;

/**
 * 日志门面接口。业务代码仅依赖本接口，不感知底层 Logback/Log4j2 实现。
 *
 * <p>所有带 {@code {} } 占位符的方法在最终输出前经脱敏器处理，保证敏感信息不落盘。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface Logger {
    /**
     * 日志输出。
     * @param msg 日志内容
     * @param args 参数
     */
    void trace(String msg, Object... args);

    /**
     * 日志输出。
     * @param msg 日志内容
     * @param args 参数
     */
    void debug(String msg, Object... args);

    /**
     * 日志输出。
     * @param msg 日志内容
     * @param args 参数
     */
    void info(String msg, Object... args);

    /**
     * 日志输出。
     * @param msg 日志内容
     * @param args 参数
     */
    void warn(String msg, Object... args);

    /**
     * 日志输出。
     * @param msg 日志内容
     * @param args 参数
     */
    void error(String msg, Object... args);

    /**
     * 日志输出。
     * @param msg 日志内容
     * @param t 异常
     * @param args 参数
     */
    void error(String msg, Throwable t, Object... args);

    /**
     * 判断是否启用指定级别（避免昂贵拼接）。
     *
     * @param level 级别，不可为 {@code null}
     * @return 启用返回 {@code true}
     */
    boolean isEnabled(LogLevel level);
}
