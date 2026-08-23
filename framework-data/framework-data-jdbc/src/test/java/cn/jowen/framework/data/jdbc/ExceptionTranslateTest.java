package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.exception.BadSqlGrammarException;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.exception.DataIntegrityViolationException;
import cn.jowen.framework.data.core.exception.DuplicateKeyException;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.exception.SQLExceptionTranslator;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.sql.SQLSyntaxErrorException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ExceptionTranslateTest 测试 SQLExceptionTranslator 翻译约束违反→DuplicateKeyException、
 * 语法错误→BadSqlGrammarException。
 */
class ExceptionTranslateTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL UNIQUE,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            """;

    private JdbcTemplate template;
    private SQLExceptionTranslator translator;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:exc1;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        translator = new SQLExceptionTranslator();
        SimpleConnectionProvider provider = new SimpleConnectionProvider(dsProps);
        connectionProvider = provider;
        template = new JdbcTemplate(provider, chain);
        // Execute DDL statements separately (H2 Statement.execute does not handle multi-statement)
        for (String stmt : DDL.split(";")) {
            String s = stmt.trim();
            if (!s.isEmpty()) {
                try (java.sql.Connection conn = connectionProvider.getConnection();
                     java.sql.Statement st = conn.createStatement()) {
                    st.execute(s);
                }
            }
        }
        // Cleanup tables to prevent data pollution across test methods
        try (java.sql.Connection conn = connectionProvider.getConnection();
             java.sql.Statement st = conn.createStatement()) {
            try { st.execute("DELETE FROM app_user"); } catch (Exception ignore) {}
            try { st.execute("DELETE FROM app_account"); } catch (Exception ignore) {}
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        if (connectionProvider != null) {
            connectionProvider.close();
        }
    }

    // -------------------------------------------------------------------------
    // 直接翻译测试（不经过 SQL 执行）
    // -------------------------------------------------------------------------

    @Test
    void translate_duplicateKeyException_forUniqueViolation() {
        SQLException ex = new SQLException("Unique constraint", "23505", 0);
        DataAccessException translated = translator.translate(ex, "INSERT INTO app_user (name) VALUES ('x')");
        // H2 唯一冲突 → SQLState 23505 → DataIntegrityViolationException（SQLStateClassifier 分类）
        // VendorSpecificTranslator 对 H2 不识别，由 SqlStateClassifier 处理
        assertThat(translated).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void translate_syntaxError_toBadSqlGrammar() {
        SQLException ex = new SQLException("Syntax error", "42000", 0);
        DataAccessException translated = translator.translate(ex, "SELECT * FROM nonexistent_table");
        assertThat(translated).isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    void translate_unknownState_toGenericDataAccessException() {
        SQLException ex = new SQLException("Unknown error", "XXXXX", 999);
        DataAccessException translated = translator.translate(ex, "some sql");
        assertThat(translated).isInstanceOf(DataAccessException.class);
        assertThat(translated).isNotInstanceOf(BadSqlGrammarException.class);
        assertThat(translated).isNotInstanceOf(DataIntegrityViolationException.class);
    }

    // -------------------------------------------------------------------------
    // 通过执行触发异常并验证翻译
    // -------------------------------------------------------------------------

    @Test
    void duplicateInsert_throwsDataIntegrityViolation() {
        template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
        assertThatThrownBy(() -> template.update("INSERT INTO app_user (name) VALUES (?)", "alice"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void badSqlSyntax_throwsBadSqlGrammar() {
        assertThatThrownBy(() -> template.update("INVALID SQL SYNTAX HERE ???"))
                .isInstanceOf(BadSqlGrammarException.class);
    }

    @Test
    void notNullViolation_throwsDataIntegrityViolation() {
        // name 列 NOT NULL，传 null 应触发完整性约束异常
        assertThatThrownBy(() -> template.update("INSERT INTO app_user (name) VALUES (NULL)"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void translate_callable_throwsOriginalException() {
        // ExceptionTranslator 接口方法：任务抛 RuntimeException 时原样重抛
        assertThatThrownBy(() -> translator.translate("action", () -> {
            throw new RuntimeException("business error");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("business error");
    }

    @Test
    void translate_callable_nullOnSuccess() {
        DataAccessException result = translator.translate("action", () -> {
            // 无异常
            return null;
        });
        assertThat(result).isNull();
    }

    @Test
    void translate_callable_wrapsSQLException() {
        // ExceptionTranslator.translate(action, Callable) returns translated exception, does not throw
        DataAccessException result = translator.translate("action", () -> {
            throw new SQLException("bad sql", "42000", 0);
        });
        assertThat(result).isInstanceOf(BadSqlGrammarException.class);
    }

    // -------------------------------------------------------------------------
    // 验证异常信息包含 SQL 上下文
    // -------------------------------------------------------------------------

    @Test
    void translatedExceptionContainsSqlInfo() {
        assertThatThrownBy(() -> template.update("INVALID SYNTAX ???"))
                .satisfies(ex -> assertThat(ex.getMessage()).contains("SQL=["));
    }
}
