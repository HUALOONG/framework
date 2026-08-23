package cn.jowen.framework.logger.facade;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LogLevel} 测试。
 */
class LogLevelTest {

    @Test
    void values_containsAllLevels() {
        LogLevel[] values = LogLevel.values();
        assertThat(values).hasSize(5);
        assertThat(values).containsExactly(
                LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR
        );
    }

    @Test
    void valueOf_trace() {
        assertThat(LogLevel.valueOf("TRACE")).isEqualTo(LogLevel.TRACE);
    }

    @Test
    void valueOf_debug() {
        assertThat(LogLevel.valueOf("DEBUG")).isEqualTo(LogLevel.DEBUG);
    }

    @Test
    void valueOf_info() {
        assertThat(LogLevel.valueOf("INFO")).isEqualTo(LogLevel.INFO);
    }

    @Test
    void valueOf_warn() {
        assertThat(LogLevel.valueOf("WARN")).isEqualTo(LogLevel.WARN);
    }

    @Test
    void valueOf_error() {
        assertThat(LogLevel.valueOf("ERROR")).isEqualTo(LogLevel.ERROR);
    }

    @Test
    void ordinal_orderCorrect() {
        assertThat(LogLevel.TRACE.ordinal()).isEqualTo(0);
        assertThat(LogLevel.DEBUG.ordinal()).isEqualTo(1);
        assertThat(LogLevel.INFO.ordinal()).isEqualTo(2);
        assertThat(LogLevel.WARN.ordinal()).isEqualTo(3);
        assertThat(LogLevel.ERROR.ordinal()).isEqualTo(4);
    }

    @Test
    void name_returnsUpperCase() {
        assertThat(LogLevel.INFO.name()).isEqualTo("INFO");
        assertThat(LogLevel.DEBUG.name()).isEqualTo("DEBUG");
    }

    @Test
    void toString_returnsName() {
        assertThat(LogLevel.WARN.toString()).isEqualTo("WARN");
    }

    @Test
    void equals_sameEnumValue() {
        assertThat(LogLevel.INFO).isEqualTo(LogLevel.INFO);
    }

    @Test
    void hashCode_consistent() {
        assertThat(LogLevel.INFO.hashCode()).isEqualTo(LogLevel.INFO.hashCode());
    }

    @Test
    void arrayConversion() {
        LogLevel[] levels = LogLevel.values();
        assertThat(Arrays.asList(levels)).containsExactly(
                LogLevel.TRACE, LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR
        );
    }
}
