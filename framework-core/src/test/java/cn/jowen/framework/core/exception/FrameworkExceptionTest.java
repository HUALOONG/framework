package cn.jowen.framework.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link FrameworkException} 及其子类测试。
 */
class FrameworkExceptionTest {

    enum TestCode implements ErrorCode {
        ERR("100", "业务失败"),
        SYS_ERR("500", "系统错误");

        TestCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        private final String code;
        private final String message;

        @Override
        public String code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }
    }

    @Test
    void frameworkException_messageOnly() {
        FrameworkException ex = new FrameworkException("msg");
        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getErrorCode()).isNull();
    }

    @Test
    void frameworkException_errorCode_only() {
        FrameworkException ex = new FrameworkException(TestCode.ERR);
        assertThat(ex.getMessage()).isEqualTo("业务失败");
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
    }

    @Test
    void frameworkException_errorCodeAndMessage() {
        FrameworkException ex = new FrameworkException(TestCode.ERR, "custom msg");
        assertThat(ex.getMessage()).isEqualTo("custom msg");
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
    }

    @Test
    void frameworkException_withCause() {
        Throwable cause = new RuntimeException("cause");
        FrameworkException ex = new FrameworkException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void businessException_getErrorCode() {
        BusinessException ex = new BusinessException(TestCode.ERR, "msg");
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
    }

    @Test
    void businessException_noErrorCode() {
        BusinessException ex = new BusinessException("msg");
        assertThat(ex.getErrorCode()).isNull();
    }

    @Test
    void businessException_withCause() {
        Throwable cause = new RuntimeException("cause");
        BusinessException ex = new BusinessException("msg", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void businessException_errorCode_only() {
        BusinessException ex = new BusinessException(TestCode.ERR);
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
        assertThat(ex.getMessage()).isEqualTo("业务失败");
    }

    @Test
    void businessException_errorCodeAndCause() {
        Throwable cause = new RuntimeException("cause");
        BusinessException ex = new BusinessException(TestCode.ERR, cause);
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void systemException_getErrorCode() {
        SystemException ex = new SystemException(TestCode.SYS_ERR, "msg");
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.SYS_ERR);
    }

    @Test
    void systemException_noErrorCode() {
        SystemException ex = new SystemException("msg");
        assertThat(ex.getErrorCode()).isNull();
    }

    @Test
    void systemException_errorCode_only() {
        // 仅错误码构造：消息取错误码默认文案
        SystemException ex = new SystemException(TestCode.SYS_ERR);
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.SYS_ERR);
        assertThat(ex.getMessage()).isEqualTo("系统错误");
    }

    @Test
    void systemException_errorCodeAndCause() {
        // 错误码 + 原因构造：错误码与原始原因均须保留
        Throwable cause = new RuntimeException("cause");
        SystemException ex = new SystemException(TestCode.SYS_ERR, cause);
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.SYS_ERR);
        assertThat(ex.getMessage()).isEqualTo("系统错误");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}