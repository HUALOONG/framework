package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.PageRequest;
import cn.jowen.framework.data.core.page.Pageable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link OracleDialect} 与 {@link SQLServerDialect} 单元测试。
 */
class OracleSqlServerDialectTest {

    // ===================== Oracle =====================

    private final OracleDialect oracle = new OracleDialect();

    @Test
    void oracle_type() {
        assertThat(oracle.type()).isEqualTo(DatabaseType.ORACLE);
    }

    @Test
    void oracle_name() {
        assertThat(oracle.name()).isEqualTo("oracle");
    }

    @Test
    void oracle_buildPageSql_firstPage() {
        Pageable pageable = PageRequest.of(1, 10).toPageable();
        String sql = oracle.buildPageSql("SELECT * FROM app_user", pageable);
        assertThat(sql).isEqualTo(
                "SELECT * FROM (SELECT a.*, ROWNUM rn__ FROM (SELECT * FROM app_user) a WHERE ROWNUM <= 10) WHERE rn__ > 0");
    }

    @Test
    void oracle_buildPageSql_secondPage() {
        Pageable pageable = PageRequest.of(2, 10).toPageable();
        String sql = oracle.buildPageSql("SELECT * FROM app_user", pageable);
        assertThat(sql).isEqualTo(
                "SELECT * FROM (SELECT a.*, ROWNUM rn__ FROM (SELECT * FROM app_user) a WHERE ROWNUM <= 20) WHERE rn__ > 10");
    }

    @Test
    void oracle_escapeIdentifier() {
        assertThat(oracle.escapeIdentifier("user_name")).isEqualTo("\"user_name\"");
    }

    // ===================== SQL Server =====================

    private final SQLServerDialect sqlServer = new SQLServerDialect();

    @Test
    void sqlServer_type() {
        assertThat(sqlServer.type()).isEqualTo(DatabaseType.SQLSERVER);
    }

    @Test
    void sqlServer_name() {
        assertThat(sqlServer.name()).isEqualTo("sqlserver");
    }

    @Test
    void sqlServer_buildPageSql_firstPage() {
        Pageable pageable = PageRequest.of(1, 10).toPageable();
        String sql = sqlServer.buildPageSql("SELECT * FROM app_user ORDER BY id", pageable);
        assertThat(sql).isEqualTo("SELECT * FROM app_user ORDER BY id OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY");
    }

    @Test
    void sqlServer_buildPageSql_secondPage() {
        Pageable pageable = PageRequest.of(2, 5).toPageable();
        String sql = sqlServer.buildPageSql("SELECT * FROM app_user ORDER BY id", pageable);
        assertThat(sql).isEqualTo("SELECT * FROM app_user ORDER BY id OFFSET 5 ROWS FETCH NEXT 5 ROWS ONLY");
    }

    @Test
    void sqlServer_escapeIdentifier() {
        assertThat(sqlServer.escapeIdentifier("user_name")).isEqualTo("[user_name]");
    }
}
