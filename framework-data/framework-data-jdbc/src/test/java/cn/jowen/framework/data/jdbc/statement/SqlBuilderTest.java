package cn.jowen.framework.data.jdbc.statement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlBuilder} 测试。
 */
class SqlBuilderTest {

    @Test
    void build_concatenatesFragmentsAndParams() {
        SqlResult result = SqlBuilder.create()
                .append("SELECT * FROM t_user WHERE id = ?", 1)
                .append(" AND name = ?", "tom")
                .append(" ORDER BY id")
                .build();

        assertThat(result.sql()).isEqualTo("SELECT * FROM t_user WHERE id = ? AND name = ? ORDER BY id");
        assertThat(result.params()).containsExactly(1, "tom");
    }

    @Test
    void append_noParams_addsOnlyFragment() {
        SqlBuilder builder = SqlBuilder.create().append("SELECT 1");
        assertThat(builder.toString()).isEqualTo("SELECT 1");

        SqlResult result = builder.build();
        assertThat(result.sql()).isEqualTo("SELECT 1");
        assertThat(result.params()).isEmpty();
    }

    @Test
    void append_withEmptyParams_keepsOrder() {
        SqlResult result = SqlBuilder.create()
                .append("UPDATE t_user SET name = ?", "x")
                .append(", age = ?", 18)
                .append(" WHERE id = ?", 7L)
                .build();

        assertThat(result.params()).containsExactly("x", 18, 7L);
    }
}
