package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 数据访问异常子类测试：覆盖 {@link DuplicateKeyException}、{@link DataIntegrityViolationException}、
 * {@link DeadlockException}、{@link TimeoutException}、{@link TransientDataAccessException}、
 * {@link BadSqlGrammarException} 的全部构造器与继承层次。
 *
 * <p>每个子类都同时断言：消息透传、原因链保留、错误码绑定，以及异常在持久层异常分类树中的归属。
 */
class DataExceptionSubclassesTest {

    enum TestCode implements ErrorCode {
        UNIQUE("D_UNIQ", "唯一键冲突");

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
    void duplicateKey_allConstructors() {
        Throwable cause = new RuntimeException("duplicate key");

        assertThat(new DuplicateKeyException("dup").getMessage()).isEqualTo("dup");
        assertThat(new DuplicateKeyException("dup", cause).getCause()).isSameAs(cause);

        DuplicateKeyException withCode = new DuplicateKeyException(TestCode.UNIQUE, "dup");
        assertThat(withCode.getErrorCode()).isEqualTo(TestCode.UNIQUE);
        assertThat(withCode.getMessage()).isEqualTo("dup");
        assertThat(withCode).isInstanceOf(DataAccessException.class);
    }

    @Test
    void dataIntegrityViolation_allConstructors() {
        Throwable cause = new SQLException("NOT NULL violation");

        DataIntegrityViolationException message = new DataIntegrityViolationException("integrity");
        assertThat(message.getMessage()).isEqualTo("integrity");
        assertThat(message).isInstanceOf(DataAccessException.class);

        DataIntegrityViolationException withCause =
                new DataIntegrityViolationException("integrity", cause);
        assertThat(withCause.getCause()).isSameAs(cause);
        assertThat(withCause).isInstanceOf(RuntimeException.class);
    }

    @Test
    void transientAllConstructors_andDeadlockIsTransient() {
        Throwable cause = new SQLException("lock wait timeout");

        TransientDataAccessException transientEx = new TransientDataAccessException("transient");
        assertThat(transientEx.getMessage()).isEqualTo("transient");
        assertThat(transientEx).isInstanceOf(DataAccessException.class);

        TransientDataAccessException withCause = new TransientDataAccessException("transient", cause);
        assertThat(withCause.getCause()).isSameAs(cause);

        // 死锁是瞬态异常的一种：重试可能成功，不应按永久错误处理
        DeadlockException deadlock = new DeadlockException("deadlock", cause);
        assertThat(deadlock).isInstanceOf(TransientDataAccessException.class);
        assertThat(deadlock.getCause()).isSameAs(cause);
        assertThat(new DeadlockException("deadlock").getMessage()).isEqualTo("deadlock");
    }

    @Test
    void timeout_allConstructors() {
        Throwable cause = new SQLException("query timeout");

        assertThat(new TimeoutException("timeout").getMessage()).isEqualTo("timeout");
        assertThat(new TimeoutException("timeout", cause).getCause()).isSameAs(cause);
    }

    @Test
    void badSqlGrammar_allConstructors() {
        Throwable cause = new SQLException("syntax error at line 1");

        BadSqlGrammarException grammar = new BadSqlGrammarException("bad sql");
        assertThat(grammar.getMessage()).isEqualTo("bad sql");
        assertThat(grammar).isInstanceOf(DataAccessException.class);

        BadSqlGrammarException withCause = new BadSqlGrammarException("bad sql", cause);
        assertThat(withCause.getCause()).isSameAs(cause);
        assertThat(withCause).isNotInstanceOf(TransientDataAccessException.class);
    }
}
