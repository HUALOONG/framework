package cn.jowen.framework.data.core.exception;

import static org.assertj.core.api.Assertions.assertThat;

import cn.jowen.framework.core.exception.ErrorCode;

import org.junit.jupiter.api.Test;

/**
 * 测试 {@link DataException} 的各类构造器、静态工厂、错误码透传与运行时异常属性。
 */
class DataExceptionTest {

    enum TestCode implements ErrorCode {
        DEMO("DATA_001", "演示错误");

        private final String code;
        private final String message;

        TestCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

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
    void isUncheckedRuntimeException() {
        assertThat(new DataException("x")).isInstanceOf(RuntimeException.class);
    }

    @Test
    void stringConstructorPassesMessage() {
        DataException ex = new DataException("boom");
        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getErrorCode()).isNull();
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void stringCauseConstructorPassesMessageAndCause() {
        Throwable cause = new IllegalStateException("root");
        DataException ex = new DataException("boom", cause);
        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void errorCodeConstructorStoresErrorCode() {
        DataException ex = new DataException(TestCode.DEMO);
        assertThat(ex.getErrorCode()).isSameAs(TestCode.DEMO);
        assertThat(ex.getMessage()).isEqualTo("演示错误");
    }

    @Test
    void errorCodeWithMessageConstructorStoresBoth() {
        DataException ex = new DataException(TestCode.DEMO, "覆盖信息");
        assertThat(ex.getErrorCode()).isSameAs(TestCode.DEMO);
        assertThat(ex.getMessage()).isEqualTo("覆盖信息");
    }

    @Test
    void errorCodeWithCauseConstructorStoresBoth() {
        Throwable cause = new IllegalStateException("root");
        DataException ex = new DataException(TestCode.DEMO, cause);
        assertThat(ex.getErrorCode()).isSameAs(TestCode.DEMO);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void ofFactoryWrapsMessageAndCause() {
        Throwable cause = new IllegalStateException("root");
        DataException ex = DataException.of("wrapped", cause);
        assertThat(ex).isInstanceOf(DataException.class);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
