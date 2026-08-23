package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.page.PageRequest;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.dialect.H2Dialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DialectTest 验证 H2Dialect.buildPageSql 分页 SQL 正确性。
 */
class DialectTest {

    private DialectRegistry registry;
    private cn.jowen.framework.data.core.dialect.DatabaseDialect h2Dialect;

    @BeforeEach
    void setUp() {
        registry = new DialectRegistry();
        h2Dialect = registry.getDefault(); // 默认 H2
        assertThat(h2Dialect.type()).isEqualTo(DatabaseType.H2);
    }

    // -------------------------------------------------------------------------
    // type / name
    // -------------------------------------------------------------------------

    @Test
    void type_returnsH2() {
        assertThat(h2Dialect.type()).isEqualTo(DatabaseType.H2);
    }

    @Test
    void name_returnsLowercase() {
        assertThat(h2Dialect.name()).isEqualTo("h2");
    }

    // -------------------------------------------------------------------------
    // buildPageSql
    // -------------------------------------------------------------------------

    @Test
    void buildPageSql_firstPage() {
        Pageable pageable = PageRequest.of(1, 10).toPageable();
        String sql = h2Dialect.buildPageSql("SELECT * FROM app_user", pageable);
        assertThat(sql).isEqualTo("SELECT * FROM app_user LIMIT 10 OFFSET 0");
    }

    @Test
    void buildPageSql_secondPage() {
        Pageable pageable = PageRequest.of(2, 10).toPageable();
        String sql = h2Dialect.buildPageSql("SELECT * FROM app_user", pageable);
        assertThat(sql).isEqualTo("SELECT * FROM app_user LIMIT 10 OFFSET 10");
    }

    @Test
    void buildPageSql_thirdPage() {
        Pageable pageable = PageRequest.of(3, 5).toPageable();
        String sql = h2Dialect.buildPageSql("SELECT * FROM app_user ORDER BY id DESC", pageable);
        assertThat(sql).isEqualTo("SELECT * FROM app_user ORDER BY id DESC LIMIT 5 OFFSET 10");
    }

    @Test
    void buildPageSql_withComplexBaseSql() {
        String base = "SELECT u.id, u.name FROM app_user u WHERE u.status = ?";
        Pageable pageable = PageRequest.of(1, 20).toPageable();
        String sql = h2Dialect.buildPageSql(base, pageable);
        assertThat(sql).isEqualTo("SELECT u.id, u.name FROM app_user u WHERE u.status = ? LIMIT 20 OFFSET 0");
    }

    // -------------------------------------------------------------------------
    // escapeIdentifier
    // -------------------------------------------------------------------------

    @Test
    void escapeIdentifier_quotesIdentifier() {
        String escaped = h2Dialect.escapeIdentifier("app_user");
        assertThat(escaped).isEqualTo("\"app_user\"");
    }

    @Test
    void escapeIdentifier_withSpecialChars() {
        String escaped = h2Dialect.escapeIdentifier("my-table");
        assertThat(escaped).isEqualTo("\"my-table\"");
    }

    // -------------------------------------------------------------------------
    // DialectRegistry defaults
    // -------------------------------------------------------------------------

    @Test
    void registryDefaultIsH2() {
        assertThat(registry.getDefault().type()).isEqualTo(DatabaseType.H2);
    }

    @Test
    void registry_containsAllBuiltInDialects() {
        assertThat(registry.get(DatabaseType.H2)).isNotNull();
        assertThat(registry.get(DatabaseType.MYSQL)).isNotNull();
        assertThat(registry.get(DatabaseType.POSTGRESQL)).isNotNull();
        assertThat(registry.get(DatabaseType.ORACLE)).isNotNull();
        assertThat(registry.get(DatabaseType.SQLSERVER)).isNotNull();
    }

    @Test
    void registry_detectByProduct_returnsH2ForUnknown() {
        // 未知数据库类型应回退 H2
        assertThat(registry.detectByProduct("UNKNOWN_DB", "1.0").type()).isEqualTo(DatabaseType.H2);
    }

    @Test
    void registry_detectByProduct_returnsNullWhenNotRegistered_thenFallback() {
        // 已注册的 MySQL 应返回 MySQL 方言
        assertThat(registry.detectByProduct("MySQL", "8.0").type()).isEqualTo(DatabaseType.MYSQL);
    }
}
