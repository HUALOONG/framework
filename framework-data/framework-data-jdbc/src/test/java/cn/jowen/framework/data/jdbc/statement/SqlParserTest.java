package cn.jowen.framework.data.jdbc.statement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SqlParser} 单元测试。
 */
class SqlParserTest {

    @Test
    void parseNamedParameters_nullOrEmpty_returnsEmpty() {
        assertThat(SqlParser.parseNamedParameters(null)).isEmpty();
        assertThat(SqlParser.parseNamedParameters("")).isEmpty();
    }

    @Test
    void parseNamedParameters_extractsOrderedUnique() {
        assertThat(SqlParser.parseNamedParameters(
                "SELECT * FROM t WHERE a = :a AND b = :b AND c = :a"))
                .containsExactly("a", "b");
    }

    @Test
    void hasNamedParameters_nullOrEmpty_returnsFalse() {
        assertThat(SqlParser.hasNamedParameters(null)).isFalse();
        assertThat(SqlParser.hasNamedParameters("")).isFalse();
    }

    @Test
    void hasNamedParameters_noParam_returnsFalse() {
        assertThat(SqlParser.hasNamedParameters("SELECT * FROM t WHERE id = 1")).isFalse();
    }

    @Test
    void hasNamedParameters_withParam_returnsTrue() {
        assertThat(SqlParser.hasNamedParameters("SELECT * FROM t WHERE id = :id")).isTrue();
    }

    @Test
    void toPositionalSql_noNamedParams_returnsOriginal() {
        // 无命名参数时原样返回
        assertThat(SqlParser.toPositionalSql("SELECT * FROM t WHERE id = ?"))
                .isEqualTo("SELECT * FROM t WHERE id = ?");
    }

    @Test
    void toPositionalSql_withNamedParams_replacesWithPlaceholder() {
        assertThat(SqlParser.toPositionalSql("WHERE a = :a AND b = :b"))
                .isEqualTo("WHERE a = ? AND b = ?");
    }
}
