package cn.jowen.framework.logger.mask;

import cn.jowen.framework.core.desensitize.Desensitizer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link LogMaskLayout} 测试。
 */
class LogMaskLayoutTest {

    @Test
    void decorate_returnsNull_whenInputNull() {
        LogMaskLayout layout = new LogMaskLayout(mock(Desensitizer.class));
        assertThat(layout.decorate(null)).isNull();
    }

    @Test
    void decorate_delegatesToDesensitizer() {
        Desensitizer desensitizer = mock(Desensitizer.class);
        when(desensitizer.mask("raw")).thenReturn("masked");
        LogMaskLayout layout = new LogMaskLayout(desensitizer);
        assertThat(layout.decorate("raw")).isEqualTo("masked");
        verify(desensitizer).mask("raw");
    }

    @Test
    void decorate_defaultConstructor_usesGlobalDesensitizer() {
        LogMaskLayout layout = new LogMaskLayout();
        assertThat(layout.decorate("hello")).isEqualTo("hello");
        assertThat(layout.decorate("13812345678")).isNotEqualTo("13812345678");
    }
}
