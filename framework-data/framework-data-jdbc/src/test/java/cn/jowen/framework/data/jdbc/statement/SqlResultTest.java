package cn.jowen.framework.data.jdbc.statement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlResult} 单元测试。
 */
class SqlResultTest {

    @Test
    void record_accessors() {
        SqlResult result = new SqlResult("select * from t where id = ?", List.of(1, "x"));
        assertThat(result.sql()).isEqualTo("select * from t where id = ?");
        assertThat(result.params()).containsExactly(1, "x");
    }

    @Test
    void toParamArray_convertsParams() {
        SqlResult result = new SqlResult("select ?", List.of(10, 20L));
        assertThat(result.toParamArray()).containsExactly(10, 20L);
    }

    @Test
    void paramCount_returnsSize() {
        SqlResult result = new SqlResult("select ?", List.of("a", "b", "c"));
        assertThat(result.paramCount()).isEqualTo(3);
    }

    @Test
    void paramCount_empty() {
        SqlResult result = new SqlResult("select 1", List.of());
        assertThat(result.paramCount()).isZero();
        assertThat(result.toParamArray()).isEmpty();
    }

    @Test
    void toString_combinesSqlAndParams() {
        SqlResult result = new SqlResult("select * from t", List.of(1, 2));
        assertThat(result.toString()).isEqualTo("select * from t [1, 2]");
    }

    @Test
    void equalsAndHashCode() {
        SqlResult a = new SqlResult("sql", List.of(1, 2));
        SqlResult b = new SqlResult("sql", List.of(1, 2));
        SqlResult c = new SqlResult("sql", List.of(1));
        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(null);
    }
}
