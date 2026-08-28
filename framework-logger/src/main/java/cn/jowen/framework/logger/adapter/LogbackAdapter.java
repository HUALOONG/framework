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
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Activate(order = 0)
@SPIImplementation(name = "logback")
public final class LogbackAdapter implements LoggerAdapter {

    /**
     * 获取 SLF4J 的门面实现。
     * @param name 名称，不可为 {@code null}
     * @return 门面实现
     */
    @Override
    public Logger getLogger(String name) {
        org.slf4j.Logger delegate = LoggerFactory.getLogger(name);
        return new Slf4jLogger(delegate);
    }

    /**
     * 基于 SLF4J 的门面实现。
     */
    @NullMarked
    private static final class Slf4jLogger implements Logger {
        /**
         * SLF4J 的门面实现。
         */
        private final org.slf4j.Logger delegate;

        /**
         * 构造函数。
         * @param delegate SLF4J 的门面实现
         */
        private Slf4jLogger(org.slf4j.Logger delegate) {
            this.delegate = delegate;
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void trace(String msg, Object... args) {
            if (delegate.isTraceEnabled()) {
                delegate.trace(msg, args);
            }
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void debug(String msg, Object... args) {
            if (delegate.isDebugEnabled()) {
                delegate.debug(msg, args);
            }
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void info(String msg, Object... args) {
            if (delegate.isInfoEnabled()) {
                delegate.info(msg, args);
            }
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void warn(String msg, Object... args) {
            if (delegate.isWarnEnabled()) {
                delegate.warn(msg, args);
            }
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void error(String msg, Object... args) {
            if (delegate.isErrorEnabled()) {
                delegate.error(msg, args);
            }
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param t 异常
         * @param args 参数
         */
        @Override
        public void error(String msg, Throwable t, Object... args) {
            if (delegate.isErrorEnabled()) {
                delegate.error(msg, t, args);
            }
        }

        /**
         * 判断指定日志级别是否启用。
         * @param level 日志级别
         * @return 是否启用
         */
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
