package cn.jowen.framework.data.mybatis.exception;

import cn.jowen.framework.data.core.exception.BadSqlGrammarException;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlexExceptionConverterTest {

    private final FlexExceptionConverter converter = FlexExceptionConverter.INSTANCE;

    @Test
    void translate_sqlSyntax() {
        DataAccessException ex = converter.translate(new RuntimeException("You have an error in your SQL syntax"));
        assertThat(ex).isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    void translate_duplicateKey() {
        DataAccessException ex = converter.translate(new RuntimeException("Duplicate entry 'abc' for key 'name'"));
        assertThat(ex).isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void translate_constraint() {
        DataAccessException ex = converter.translate(new RuntimeException("Constraint violation: NOT NULL"));
        assertThat(ex).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void translate_optimisticLock() {
        DataAccessException ex = converter.translate(new RuntimeException("optimistic lock version conflict"));
        assertThat(ex).isInstanceOf(FlexOptimisticLockException.class);
    }

    @Test
    void translate_generic() {
        DataAccessException ex = converter.translate(new RuntimeException("some random error"));
        assertThat(ex).isInstanceOf(DataAccessException.class);
        assertThat(ex).isNotInstanceOf(BadSqlGrammarException.class);
        assertThat(ex).isNotInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void translate_null() {
        DataAccessException ex = converter.translate(null);
        assertThat(ex).isNotNull();
    }

    @Test
    void translate_message1062() {
        DataAccessException ex = converter.translate(new RuntimeException("MySQL error 1062: Duplicate entry"));
        assertThat(ex).isInstanceOf(DuplicateKeyException.class);
    }
}
