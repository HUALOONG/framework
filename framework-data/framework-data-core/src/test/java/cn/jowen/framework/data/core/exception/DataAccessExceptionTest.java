package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataAccessException} 测试：覆盖全部构造器分支。
 */
class DataAccessExceptionTest {

    enum TestCode implements ErrorCode {
        ERR("D1", "data err"),
        ERR2("D2", "data err2");

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
    void messageOnly() {
        DataAccessException ex = new DataAccessException("boom");
        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getErrorCode()).isNull();
    }

    @Test
    void messageAndCause() {
        Throwable cause = new RuntimeException("c");
        DataAccessException ex = new DataAccessException("boom", cause);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void errorCodeOnly() {
        DataAccessException ex = new DataAccessException(TestCode.ERR);
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
    }

    @Test
    void errorCodeAndMessage() {
        DataAccessException ex = new DataAccessException(TestCode.ERR, "custom");
        assertThat(ex.getMessage()).isEqualTo("custom");
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR);
    }

    @Test
    void errorCodeAndCause() {
        Throwable cause = new RuntimeException("c");
        DataAccessException ex = new DataAccessException(TestCode.ERR2, cause);
        assertThat(ex.getErrorCode()).isEqualTo(TestCode.ERR2);
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
