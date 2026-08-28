package cn.jowen.framework.logger.layout;

import cn.jowen.framework.logger.facade.LogLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StructuredLayout} 测试。
 */
class StructuredLayoutTest {

    private final StructuredLayout layout = new StructuredLayout();

    @Test
    void formatText_includesLevelLoggerAndMessage() {
        String line = layout.formatText("com.app.Service", LogLevel.INFO, "hello", new Object[0]);
        assertThat(line)
                .contains("[INFO]")
                .contains("com.app.Service")
                .contains("hello")
                .endsWith(System.lineSeparator());
    }

    @Test
    void formatText_fillsPlaceholders() {
        String line = layout.formatText("app", LogLevel.DEBUG, "user {} login from {}", new Object[]{"tom", "10.0.0.1"});
        assertThat(line).contains("user tom login from 10.0.0.1");
    }

    @Test
    void formatText_appendsExtraArgsWhenPlaceholdersExhausted() {
        String line = layout.formatText("app", LogLevel.WARN, "done", new Object[]{"a", "b"});
        assertThat(line).contains("done [a, b]");
    }

    @Test
    void formatText_dropsUnfilledPlaceholders() {
        String line = layout.formatText("app", LogLevel.ERROR, "{} {}", new Object[]{"x"});
        assertThat(line).contains("app - x ");
    }

    @Test
    void formatText_nullMessage_usesEmptyText() {
        String line = layout.formatText("app", LogLevel.INFO, null, new Object[0]);
        assertThat(line).contains("app - ");
    }
}
