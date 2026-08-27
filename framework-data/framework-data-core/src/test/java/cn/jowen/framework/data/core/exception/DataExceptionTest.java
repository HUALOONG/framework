package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DataExceptionTest {

    @Test
    void messageOnly() {
        DataException ex = new DataException("error");
        assertThat(ex.getMessage()).isEqualTo("error");
    }

    @Test
    void withCause() {
        Throwable cause = new RuntimeException("root");
        DataException ex = new DataException("wrapped", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void withErrorCode() {
        ErrorCode code = new ErrorCode() {
            @Override public String code() { return "DATA_001"; }
            @Override public String message() { return "data error"; }
        };
        DataException ex = new DataException(code);
        assertThat(ex.getMessage()).isEqualTo("data error");
    }

    @Test
    void withErrorCodeAndMessage() {
        ErrorCode code = new ErrorCode() {
            @Override public String code() { return "DATA_001"; }
            @Override public String message() { return "data error"; }
        };
        DataException ex = new DataException(code, "specific");
        assertThat(ex.getMessage()).isEqualTo("specific");
    }

    @Test
    void of_withCause() {
        Throwable cause = new RuntimeException("root");
        DataException ex = DataException.of("msg", cause);
        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void of_withNullCause() {
        DataException ex = DataException.of("msg", null);
        assertThat(ex.getMessage()).isEqualTo("msg");
        assertThat(ex.getCause()).isNull();
    }
}
