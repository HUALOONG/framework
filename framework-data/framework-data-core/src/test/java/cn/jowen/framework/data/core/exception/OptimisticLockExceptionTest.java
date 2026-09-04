package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.ErrorCode;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OptimisticLockExceptionTest {

    enum TestCode implements ErrorCode {
        OPT("E9001", "乐观锁冲突");

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
    void constructors() {
        OptimisticLockException e1 = new OptimisticLockException("conflict");
        assertThat(e1.getMessage()).isEqualTo("conflict");

        OptimisticLockException e2 = new OptimisticLockException("conflict", new RuntimeException("c"));
        assertThat(e2.getMessage()).isEqualTo("conflict");
        assertThat(e2.getCause()).hasMessage("c");

        OptimisticLockException e3 = new OptimisticLockException(TestCode.OPT, "custom");
        assertThat(e3.getMessage()).isEqualTo("custom");
        assertThat(e3.getErrorCode()).isEqualTo(TestCode.OPT);
    }
}
