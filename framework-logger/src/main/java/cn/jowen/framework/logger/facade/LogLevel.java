package cn.jowen.framework.logger.facade;

import org.jspecify.annotations.NullMarked;

/**
 * 日志级别枚举，与常见实现（Logback/Log4j2/SLF4J）对齐。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public enum LogLevel {
    /**
     * 跟踪日志。
     */
    TRACE,
    /**
     * 调试日志。
     */
    DEBUG,
    /**
     * 信息日志。
     */
    INFO,
    /**
     * 警告日志。
     */
    WARN,
    /**
     * 错误日志。
     */
    ERROR
}
