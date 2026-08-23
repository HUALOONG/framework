package cn.jowen.framework.core.desensitize;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link DesensitizeException} 测试。
 */
class DesensitizeExceptionTest {

    @Test
    void desensitizeException_messageOnly() {
        DesensitizeException ex = new DesensitizeException("未知策略");
        assertThat(ex.getMessage()).isEqualTo("未知策略");
    }

    @Test
    void desensitizeException_withCause() {
        RuntimeException cause = new RuntimeException("root");
        DesensitizeException ex = new DesensitizeException("失败", cause);
        assertThat(ex.getMessage()).isEqualTo("失败");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void desensitizeException_nullMessage() {
        DesensitizeException ex = new DesensitizeException((String) null);
        assertThat(ex.getMessage()).isNull();
    }

    @Test
    void desensitizeException_isFrameworkException() {
        DesensitizeException ex = new DesensitizeException("msg");
        assertThat(ex).isInstanceOf(cn.jowen.framework.core.exception.FrameworkException.class);
    }
}