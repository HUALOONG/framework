package cn.jowen.framework.logger.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LoggerProperties} 测试。
 */
class LoggerPropertiesTest {

    private final LoggerProperties properties = new LoggerProperties();

    @Test
    void defaultValues() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.isAsyncEnabled()).isTrue();
        assertThat(properties.getLevel()).isEqualTo("info");
        assertThat(properties.getFormat()).isEqualTo("text");
        assertThat(properties.isDesensitizeEnabled()).isTrue();
    }

    @Test
    void setEnabled_getEnabled() {
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
        properties.setEnabled(true);
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void setAsyncEnabled_getAsyncEnabled() {
        properties.setAsyncEnabled(false);
        assertThat(properties.isAsyncEnabled()).isFalse();
        properties.setAsyncEnabled(true);
        assertThat(properties.isAsyncEnabled()).isTrue();
    }

    @Test
    void setLevel_getLevel() {
        properties.setLevel("debug");
        assertThat(properties.getLevel()).isEqualTo("debug");
        properties.setLevel("trace");
        assertThat(properties.getLevel()).isEqualTo("trace");
        properties.setLevel("error");
        assertThat(properties.getLevel()).isEqualTo("error");
    }

    @Test
    void setFormat_getFormat() {
        properties.setFormat("json");
        assertThat(properties.getFormat()).isEqualTo("json");
        properties.setFormat("text");
        assertThat(properties.getFormat()).isEqualTo("text");
    }

    @Test
    void setDesensitizeEnabled_getDesensitizeEnabled() {
        properties.setDesensitizeEnabled(false);
        assertThat(properties.isDesensitizeEnabled()).isFalse();
        properties.setDesensitizeEnabled(true);
        assertThat(properties.isDesensitizeEnabled()).isTrue();
    }

    @Test
    void allSettersAndGettersWorkTogether() {
        properties.setEnabled(false);
        properties.setAsyncEnabled(false);
        properties.setLevel("warn");
        properties.setFormat("json");
        properties.setDesensitizeEnabled(false);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.isAsyncEnabled()).isFalse();
        assertThat(properties.getLevel()).isEqualTo("warn");
        assertThat(properties.getFormat()).isEqualTo("json");
        assertThat(properties.isDesensitizeEnabled()).isFalse();
    }

    @Test
    void level_casePreserved() {
        properties.setLevel("INFO");
        assertThat(properties.getLevel()).isEqualTo("INFO");
    }

    @Test
    void format_jsonAndText() {
        properties.setFormat("json");
        assertThat(properties.getFormat()).isEqualTo("json");
        properties.setFormat("text");
        assertThat(properties.getFormat()).isEqualTo("text");
    }
}
