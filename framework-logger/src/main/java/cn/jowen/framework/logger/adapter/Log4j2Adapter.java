package cn.jowen.framework.logger.adapter;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.logger.facade.LogLevel;
import cn.jowen.framework.logger.facade.Logger;
import org.jspecify.annotations.NullMarked;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.AbstractLogger;

/**
 * 可选日志适配器，桥接 Log4j2（经 SLF4J 绑定 {@code log4j-slf4j-impl}）。order 高于 Logback，存在时优先。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Activate(order = -10)
@SPIImplementation(name = "log4j2")
public final class Log4j2Adapter implements LoggerAdapter {

    @Override
    public Logger getLogger(String name) {
        org.apache.logging.log4j.Logger delegate = LogManager.getLogger(name);
        return new Log4jLogger(delegate);
    }

    /** 基于 Log4j2 的门面实现。 */
    @NullMarked
    private static final class Log4jLogger implements Logger {

        private final org.apache.logging.log4j.Logger delegate;

        private Log4jLogger(org.apache.logging.log4j.Logger delegate) {
            this.delegate = delegate;
        }

        @Override
        public void trace(String msg, Object... args) {
            delegate.trace(msg, args);
        }

        @Override
        public void debug(String msg, Object... args) {
            delegate.debug(msg, args);
        }

        @Override
        public void info(String msg, Object... args) {
            delegate.info(msg, args);
        }

        @Override
        public void warn(String msg, Object... args) {
            delegate.warn(msg, args);
        }

        @Override
        public void error(String msg, Object... args) {
            delegate.error(msg, args);
        }

        @Override
        public void error(String msg, Throwable t, Object... args) {
            delegate.error(msg, t, args);
        }

        @Override
        public boolean isEnabled(LogLevel level) {
            return switch (level) {
                case TRACE -> delegate.isTraceEnabled();
                case DEBUG -> delegate.isDebugEnabled();
                case INFO -> delegate.isInfoEnabled();
                case WARN -> delegate.isWarnEnabled();
                case ERROR -> delegate.isErrorEnabled();
            };
        }
    }
}
