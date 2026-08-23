package cn.jowen.framework.data.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 数据访问异常体系测试。
 */
class DataAccessExceptionTest {

    @Test
    void dataAccessException_messageOnly() {
        DataAccessException ex = new DataAccessException("test error");
        assertThat(ex.getMessage()).isEqualTo("test error");
    }

    @Test
    void dataAccessException_withCause() {
        Throwable cause = new RuntimeException("root cause");
        DataAccessException ex = new DataAccessException("wrapped", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void duplicateKeyException_message() {
        DuplicateKeyException ex = new DuplicateKeyException("duplicate key");
        assertThat(ex.getMessage()).isEqualTo("duplicate key");
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void optimisticLockException_message() {
        OptimisticLockException ex = new OptimisticLockException("optimistic lock failed");
        assertThat(ex.getMessage()).isEqualTo("optimistic lock failed");
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void badSqlGrammarException_message() {
        BadSqlGrammarException ex = new BadSqlGrammarException("SQL grammar error");
        assertThat(ex.getMessage()).isEqualTo("SQL grammar error");
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void dataIntegrityViolationException_message() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("integrity violation");
        assertThat(ex.getMessage()).isEqualTo("integrity violation");
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void transientDataAccessException_message() {
        TransientDataAccessException ex = new TransientDataAccessException("transient error");
        assertThat(ex.getMessage()).isEqualTo("transient error");
        assertThat(ex).isInstanceOf(DataAccessException.class);
    }

    @Test
    void deadlockException_message() {
        DeadlockException ex = new DeadlockException("deadlock");
        assertThat(ex.getMessage()).isEqualTo("deadlock");
        assertThat(ex).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void timeoutException_message() {
        TimeoutException ex = new TimeoutException("timeout");
        assertThat(ex.getMessage()).isEqualTo("timeout");
        assertThat(ex).isInstanceOf(TransientDataAccessException.class);
    }
}
