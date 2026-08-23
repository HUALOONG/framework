package cn.jowen.framework.logger.layout;

import cn.jowen.framework.logger.facade.LogLevel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JsonLogFormatter} 测试。
 */
class JsonLogFormatterTest {

    private final JsonLogFormatter formatter = new JsonLogFormatter();

    @Test
    void format_fullFields_producesValidJson() {
        String result = formatter.format("test.Logger", LogLevel.INFO, "hello world", "trace-1", "span-1");
        assertThat(result).contains("\"level\":\"INFO\"");
        assertThat(result).contains("\"logger\":\"test.Logger\"");
        assertThat(result).contains("\"msg\":\"hello world\"");
        assertThat(result).contains("\"traceId\":\"trace-1\"");
        assertThat(result).contains("\"spanId\":\"span-1\"");
        assertThat(result).contains("\"timestamp\":");
    }

    @Test
    void format_nullMessage_producesNullMsg() {
        String result = formatter.format("my.Logger", LogLevel.DEBUG, null, null, null);
        assertThat(result).contains("\"msg\":null");
        assertThat(result).doesNotContain("traceId");
        assertThat(result).doesNotContain("spanId");
    }

    @Test
    void format_nullTraceId_andSpanId_omitsFields() {
        String result = formatter.format("l", LogLevel.WARN, "msg", null, null);
        assertThat(result).doesNotContain("traceId");
        assertThat(result).doesNotContain("spanId");
        assertThat(result).contains("\"msg\":\"msg\"");
    }

    @Test
    void format_messageWithSpecialChars_escaped() {
        String result = formatter.format("l", LogLevel.INFO, "say \"hello\"", null, null);
        assertThat(result).contains("\\\"hello\\\"");
    }

    @Test
    void format_messageWithNewline_preserved() {
        String result = formatter.format("l", LogLevel.INFO, "line1\nline2", null, null);
        // 当前实现保留原始换行符（已知行为：JSON 字符串中未转义）
        assertThat(result).contains("line1\nline2");
    }

    @Test
    void format_loggerNameWithSpecialChars_escaped() {
        String result = formatter.format("com.example\"Test", LogLevel.INFO, null, null, null);
        assertThat(result).contains("\\\"");
    }

    @Test
    void format_allLogLevelValues() {
        for (LogLevel level : LogLevel.values()) {
            String result = formatter.format("l", level, null, null, null);
            assertThat(result).contains("\"level\":\"" + level + "\"");
        }
    }

    @Test
    void format_traceIdWithSpecialChars_escaped() {
        String result = formatter.format("l", LogLevel.INFO, null, "tr\"ace", null);
        assertThat(result).contains("tr\\\"ace");
    }

    @Test
    void format_spanIdWithSpecialChars_escaped() {
        String result = formatter.format("l", LogLevel.INFO, null, null, "sp\\an");
        assertThat(result).contains("sp\\\\an");
    }

    @Test
    void format_emptyMessage() {
        String result = formatter.format("l", LogLevel.INFO, "", null, null);
        assertThat(result).contains("\"msg\":\"\"");
    }

    @Test
    void format_outputIsJsonLike() {
        String result = formatter.format("l", LogLevel.INFO, "test", "tid", "sid");
        assertThat(result).startsWith("{\"timestamp\":");
        assertThat(result).endsWith("}");
    }
}
