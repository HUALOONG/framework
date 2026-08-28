package cn.jowen.framework.extras.web.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WebExceptionTest {

    @Test
    void constructor_withMessage_setsMessage() {
        WebException e = new WebException("业务异常");
        assertThat(e.getMessage()).isEqualTo("业务异常");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void constructor_withMessageAndCause_setsBoth() {
        IllegalStateException cause = new IllegalStateException("root");
        WebException e = new WebException("业务异常", cause);
        assertThat(e.getMessage()).isEqualTo("业务异常");
        assertThat(e.getCause()).isSameAs(cause);
    }
}
