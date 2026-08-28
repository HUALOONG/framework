package cn.jowen.framework.data.jdbc.exception;

import cn.jowen.framework.data.core.exception.BadSqlGrammarException;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DeadlockException;
import cn.jowen.framework.data.core.exception.TimeoutException;
import cn.jowen.framework.data.core.exception.TransientDataAccessException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlStateClassifier} 测试。
 */
class SqlStateClassifierTest {

    @Test
    void nullState_generic() {
        SQLException ex = sql(null);
        assertThat(SqlStateClassifier.classify(ex, "SELECT 1")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void integrity_violations() {
        assertThat(SqlStateClassifier.classify(sql("23505"), "INSERT")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(SqlStateClassifier.classify(sql("23503"), "INSERT")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(SqlStateClassifier.classify(sql("23000"), "INSERT")).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(SqlStateClassifier.classify(sql("23900"), "INSERT")).isInstanceOf(DataIntegrityViolationException.class);
        // 默认分支中 startsWith("23")
        assertThat(SqlStateClassifier.classify(sql("23999"), "INSERT")).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void transient_rollback() {
        assertThat(SqlStateClassifier.classify(sql("40001"), "UPDATE")).isInstanceOf(DeadlockException.class);
        assertThat(SqlStateClassifier.classify(sql("40002"), "UPDATE")).isInstanceOf(TransientDataAccessException.class);
        assertThat(SqlStateClassifier.classify(sql("40P01"), "UPDATE")).isInstanceOf(TransientDataAccessException.class);
        assertThat(SqlStateClassifier.classify(sql("40"), "UPDATE")).isInstanceOf(TransientDataAccessException.class);
        assertThat(SqlStateClassifier.classify(sql("40999"), "UPDATE")).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void transient_connection() {
        assertThat(SqlStateClassifier.classify(sql("08"), "SELECT")).isInstanceOf(TransientDataAccessException.class);
        assertThat(SqlStateClassifier.classify(sql("08006"), "SELECT")).isInstanceOf(TransientDataAccessException.class);
    }

    @Test
    void badSqlGrammar() {
        assertThat(SqlStateClassifier.classify(sql("42"), "SELECT")).isInstanceOf(BadSqlGrammarException.class);
        assertThat(SqlStateClassifier.classify(sql("42000"), "SELECT")).isInstanceOf(BadSqlGrammarException.class);
        assertThat(SqlStateClassifier.classify(sql("HY000"), "SELECT")).isInstanceOf(BadSqlGrammarException.class);
        assertThat(SqlStateClassifier.classify(sql("42999"), "SELECT")).isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    void timeout() {
        assertThat(SqlStateClassifier.classify(sql("S1T00"), "SELECT")).isInstanceOf(TimeoutException.class);
        assertThat(SqlStateClassifier.classify(sql("57014"), "SELECT")).isInstanceOf(TimeoutException.class);
    }

    @Test
    void unknownState_generic() {
        assertThat(SqlStateClassifier.classify(sql("01000"), "SELECT")).isInstanceOf(DataAccessException.class);
    }

    private static SQLException sql(String state) {
        return new SQLException("boom", state);
    }
}
