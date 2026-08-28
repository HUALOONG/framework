package cn.jowen.framework.logger.adapter;

import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.logger.facade.LogLevel;
import cn.jowen.framework.logger.facade.Logger;
import org.apache.logging.log4j.LogManager;
import org.jspecify.annotations.NullMarked;

/**
 * 可选日志适配器，桥接 Log4j2（经 SLF4J 绑定 {@code log4j-slf4j-impl}）。order 高于 Logback，存在时优先。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Activate(order = -10)
@SPIImplementation(name = "log4j2")
public final class Log4j2Adapter implements LoggerAdapter {

    /**
     * 获取 Log4j2 的门面实现。
     * @param name 名称，不可为 {@code null}
     * @return 门面实现
     */
    @Override
    public Logger getLogger(String name) {
        org.apache.logging.log4j.Logger delegate = LogManager.getLogger(name);
        return new Log4jLogger(delegate);
    }

    /**
     * 基于 Log4j2 的门面实现。
     */
    @NullMarked
    private static final class Log4jLogger implements Logger {
        /**
         * Log4j2 的门面实现。
         */
        private final org.apache.logging.log4j.Logger delegate;

        /**
         * 构造函数。
         * @param delegate Log4j2 的门面实现
         */
        private Log4jLogger(org.apache.logging.log4j.Logger delegate) {
            this.delegate = delegate;
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void trace(String msg, Object... args) {
            delegate.trace(msg, args);
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void debug(String msg, Object... args) {
            delegate.debug(msg, args);
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void info(String msg, Object... args) {
            delegate.info(msg, args);
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void warn(String msg, Object... args) {
            delegate.warn(msg, args);
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param args 参数
         */
        @Override
        public void error(String msg, Object... args) {
            delegate.error(msg, args);
        }

        /**
         * 日志输出。
         * @param msg 日志内容
         * @param t 异常
         * @param args 参数
         */
        @Override
        public void error(String msg, Throwable t, Object... args) {
            delegate.error(msg, t, args);
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
