package cn.jowen.framework.plugin.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationErrorTest {

    @Test
    void defaultSeverity_isError() {
        ValidationError err = new ValidationError("field", "message");
        assertThat(err.severity()).isEqualTo(ValidationError.Severity.ERROR);
    }

    @Test
    void explicitSeverity() {
        ValidationError err = new ValidationError("field", "message", ValidationError.Severity.WARNING);
        assertThat(err.severity()).isEqualTo(ValidationError.Severity.WARNING);
    }

    @Test
    void fieldAndMessage() {
        ValidationError err = new ValidationError("pluginId", "不能为空");
        assertThat(err.field()).isEqualTo("pluginId");
        assertThat(err.message()).isEqualTo("不能为空");
    }
}
