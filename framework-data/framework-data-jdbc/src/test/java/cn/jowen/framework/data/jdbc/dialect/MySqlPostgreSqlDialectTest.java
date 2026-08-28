package cn.jowen.framework.data.jdbc.dialect;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.PageRequest;
import cn.jowen.framework.data.core.page.Pageable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MySQLDialect} 与 {@link PostgreSQLDialect} 单元测试。
 */
class MySqlPostgreSqlDialectTest {

    // ===================== MySQL =====================

    private final MySQLDialect mysql = new MySQLDialect();

    @Test
    void mysql_type() {
        assertThat(mysql.type()).isEqualTo(DatabaseType.MYSQL);
    }

    @Test
    void mysql_name() {
        assertThat(mysql.name()).isEqualTo("mysql");
    }

    @Test
    void mysql_escapeIdentifier() {
        assertThat(mysql.escapeIdentifier("user_name")).isEqualTo("`user_name`");
    }

    @Test
    void mysql_buildPageSql_inheritsLimitOffset() {
        Pageable pageable = PageRequest.of(2, 5).toPageable();
        assertThat(mysql.buildPageSql("SELECT * FROM app_user", pageable))
                .isEqualTo("SELECT * FROM app_user LIMIT 5 OFFSET 5");
    }

    // ===================== PostgreSQL =====================

    private final PostgreSQLDialect pg = new PostgreSQLDialect();

    @Test
    void pg_type() {
        assertThat(pg.type()).isEqualTo(DatabaseType.POSTGRESQL);
    }

    @Test
    void pg_name() {
        assertThat(pg.name()).isEqualTo("postgresql");
    }

    @Test
    void pg_escapeIdentifier() {
        assertThat(pg.escapeIdentifier("user_name")).isEqualTo("\"user_name\"");
    }

    @Test
    void pg_buildPageSql_inheritsLimitOffset() {
        Pageable pageable = PageRequest.of(1, 20).toPageable();
        assertThat(pg.buildPageSql("SELECT * FROM app_user", pageable))
                .isEqualTo("SELECT * FROM app_user LIMIT 20 OFFSET 0");
    }
}
