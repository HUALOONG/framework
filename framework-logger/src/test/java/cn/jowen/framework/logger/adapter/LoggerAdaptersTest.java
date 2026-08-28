package cn.jowen.framework.logger.adapter;

import cn.jowen.framework.logger.facade.LogLevel;
import cn.jowen.framework.logger.facade.Logger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * {@link LogbackAdapter} 与 {@link Log4j2Adapter} 测试。
 */
class LoggerAdaptersTest {

    @Test
    void logbackAdapter_getLogger_returnsNonNullLogger() {
        Logger logger = new LogbackAdapter().getLogger("test.logback");
        assertThat(logger).isNotNull();
    }

    @Test
    void logbackAdapter_allLogMethods_doNotThrow() {
        Logger logger = new LogbackAdapter().getLogger("test.logback");
        assertThatCode(() -> {
            logger.trace("trace {}", 1);
            logger.debug("debug {}", 1);
            logger.info("info {}", 1);
            logger.warn("warn {}", 1);
            logger.error("error {}", 1);
            logger.error("error with cause {}", new IllegalStateException("boom"), 1);
        }).doesNotThrowAnyException();
    }

    @Test
    void logbackAdapter_isEnabled_acceptsAllLevels() {
        Logger logger = new LogbackAdapter().getLogger("test.logback");
        for (LogLevel level : LogLevel.values()) {
            assertThatCode(() -> logger.isEnabled(level)).doesNotThrowAnyException();
        }
    }

    @Test
    void log4j2Adapter_getLogger_returnsNonNullLogger() {
        Logger logger = new Log4j2Adapter().getLogger("test.log4j2");
        assertThat(logger).isNotNull();
    }

    @Test
    void log4j2Adapter_allLogMethods_doNotThrow() {
        Logger logger = new Log4j2Adapter().getLogger("test.log4j2");
        assertThatCode(() -> {
            logger.trace("trace {}", 1);
            logger.debug("debug {}", 1);
            logger.info("info {}", 1);
            logger.warn("warn {}", 1);
            logger.error("error {}", 1);
            logger.error("error with cause {}", new IllegalStateException("boom"), 1);
        }).doesNotThrowAnyException();
    }

    @Test
    void log4j2Adapter_isEnabled_acceptsAllLevels() {
        Logger logger = new Log4j2Adapter().getLogger("test.log4j2");
        for (LogLevel level : LogLevel.values()) {
            assertThatCode(() -> logger.isEnabled(level)).doesNotThrowAnyException();
        }
    }
}
