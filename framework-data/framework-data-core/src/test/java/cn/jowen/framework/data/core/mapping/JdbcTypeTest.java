package cn.jowen.framework.data.core.mapping;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class JdbcTypeTest {

    @Test
    void getCode_matchesJdbcConstant() {
        assertThat(JdbcType.VARCHAR.getCode()).isEqualTo(java.sql.Types.VARCHAR);
        assertThat(JdbcType.INTEGER.getCode()).isEqualTo(java.sql.Types.INTEGER);
        assertThat(JdbcType.BIGINT.getCode()).isEqualTo(java.sql.Types.BIGINT);
        assertThat(JdbcType.BOOLEAN.getCode()).isEqualTo(java.sql.Types.BIT);
    }

    @Test
    void of_knownType() {
        assertThat(JdbcType.of(java.sql.Types.VARCHAR)).isEqualTo(JdbcType.VARCHAR);
        assertThat(JdbcType.of(java.sql.Types.INTEGER)).isEqualTo(JdbcType.INTEGER);
        assertThat(JdbcType.of(java.sql.Types.BIGINT)).isEqualTo(JdbcType.BIGINT);
        assertThat(JdbcType.of(java.sql.Types.TIMESTAMP)).isEqualTo(JdbcType.TIMESTAMP);
        assertThat(JdbcType.of(java.sql.Types.BLOB)).isEqualTo(JdbcType.BLOB);
        assertThat(JdbcType.of(java.sql.Types.DECIMAL)).isEqualTo(JdbcType.DECIMAL);
    }

    @Test
    void of_unknownType_returnsOther() {
        assertThat(JdbcType.of(Integer.MAX_VALUE)).isEqualTo(JdbcType.OTHER);
        assertThat(JdbcType.of(987654321)).isEqualTo(JdbcType.OTHER);
    }
}
