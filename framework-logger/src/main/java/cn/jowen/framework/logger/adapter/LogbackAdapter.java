package cn.jowen.framework.logger.adapter;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.logger.facade.LogLevel;
import cn.jowen.framework.logger.facade.Logger;
import org.jspecify.annotations.NullMarked;
import org.slf4j.LoggerFactory;

/**
 * 默认日志适配器，桥接 SLF4J/Logback。当 classpath 存在 SLF4J 时由 {@link LoggerFactory} 自动激活。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Activate(order = 0)
@SPIImplementation(name = "logback")
public final class LogbackAdapter implements LoggerAdapter {

    @Override
    public Logger getLogger(String name) {
        org.slf4j.Logger delegate = LoggerFactory.getLogger(name);
        return new Slf4jLogger(delegate);
    }

    /** 基于 SLF4J 的门面实现。 */
    @NullMarked
    private static final class Slf4jLogger implements Logger {

        private final org.slf4j.Logger delegate;

        private Slf4jLogger(org.slf4j.Logger delegate) {
            this.delegate = delegate;
        }

        @Override
        public void trace(String msg, Object... args) {
            if (delegate.isTraceEnabled()) {
                delegate.trace(msg, args);
            }
        }

        @Override
        public void debug(String msg, Object... args) {
            if (delegate.isDebugEnabled()) {
                delegate.debug(msg, args);
            }
        }

        @Override
        public void info(String msg, Object... args) {
            if (delegate.isInfoEnabled()) {
                delegate.info(msg, args);
            }
        }

        @Override
        public void warn(String msg, Object... args) {
            if (delegate.isWarnEnabled()) {
                delegate.warn(msg, args);
            }
        }

        @Override
        public void error(String msg, Object... args) {
            if (delegate.isErrorEnabled()) {
                delegate.error(msg, args);
            }
        }

        @Override
        public void error(String msg, Throwable t, Object... args) {
            if (delegate.isErrorEnabled()) {
                delegate.error(msg, t, args);
            }
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
