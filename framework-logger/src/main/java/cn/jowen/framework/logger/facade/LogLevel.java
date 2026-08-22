package cn.jowen.framework.logger.facade;

import org.jspecify.annotations.NullMarked;

/**
 * 日志级别枚举，与常见实现（Logback/Log4j2/SLF4J）对齐。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}
