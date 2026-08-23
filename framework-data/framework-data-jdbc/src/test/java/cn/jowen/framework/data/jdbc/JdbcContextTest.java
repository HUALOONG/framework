package cn.jowen.framework.data.jdbc;

import java.util.List;
import java.util.Map;

import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import cn.jowen.framework.data.jdbc.context.JdbcContextBuilder;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.core.NamedParameterTemplate;
import cn.jowen.framework.data.jdbc.core.BatchTemplate;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.core.repository.Repository;

import cn.jowen.framework.data.jdbc.transaction.JdbcTransactionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcContextTest 测试独立启动流程（无 Spring）与 close 关闭连接池。
 */
class JdbcContextTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            """;

    private JdbcContext ctx;
    private ConnectionProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        ctx = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:ctx1;DB_CLOSE_DELAY=-1")
                .username("sa").password("")
                .poolType(PoolType.SIMPLE)
                .build();
        provider = ctx.getConnectionProvider();
// Execute DDL statements separately
        for (String stmt : DDL.split(";")) {
            String s = stmt.trim();
            if (!s.isEmpty()) {
                try (java.sql.Connection conn = provider.getConnection();
                     java.sql.Statement st = conn.createStatement()) {
                    st.execute(s);
                }
            }
        }
        // Cleanup tables to prevent data pollution across test methods
        try (java.sql.Connection conn = ctx.getConnectionProvider().getConnection();
             java.sql.Statement st = conn.createStatement()) {
            try { st.execute("DELETE FROM app_user"); } catch (Exception ignore) {}
            try { st.execute("DELETE FROM app_account"); } catch (Exception ignore) {}
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        ctx.close();
    }

    // -------------------------------------------------------------------------
    // 独立启动：所有组件均可获取
    // -------------------------------------------------------------------------

    @Test
    void contextBuildsAllComponents() {
        assertThat(ctx.getJdbcTemplate()).isNotNull();
        assertThat(ctx.getNamedParameterTemplate()).isNotNull();
        assertThat(ctx.getBatchTemplate()).isNotNull();
        assertThat(ctx.getTransactionManager()).isNotNull();
        assertThat(ctx.getConnectionProvider()).isNotNull();
        assertThat(ctx.getDialectRegistry()).isNotNull();
    }

    @Test
    void jdbcTemplateWorks() {
        JdbcTemplate template = ctx.getJdbcTemplate();
        template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void namedParameterTemplateWorks() {
        NamedParameterTemplate named = ctx.getNamedParameterTemplate();
        named.update("INSERT INTO app_user (name) VALUES (:name)", java.util.Map.of("name", "bob"));
        Long count = ctx.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void batchTemplateWorks() {
        BatchTemplate batch = ctx.getBatchTemplate();
        int[] results = batch
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "alice")
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "bob")
                .executeBatch();
        assertThat(results).hasSize(2);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
    }

    @Test
    void repositoryWorks() {
        Repository<User, Object> repo = ctx.getRepository(User.class);
        User saved = new User("alice", 1, null);
        repo.save(saved);
        assertThat(saved.getId()).isNotNull();
        Long count = ctx.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void transactionManagerWorks() {
        JdbcTransactionManager tm = ctx.getTransactionManager();
        assertThat(tm).isNotNull();
    }

    @Test
    void dialectRegistryDefaultIsH2() {
        assertThat(ctx.getDialectRegistry().getDefault().type().name()).isEqualTo("H2");
    }

    // -------------------------------------------------------------------------
    // close 关闭连接池
    // -------------------------------------------------------------------------

    @Test
    void close_disposesConnectionProvider() {
        ConnectionProvider prov = ctx.getConnectionProvider();
        assertThat(prov.isClosed()).isFalse();
        ctx.close();
        assertThat(prov.isClosed()).isTrue();
    }

    @Test
    void operationsFailAfterClose() {
        ctx.close();
        assertThatThrownBy(() -> ctx.getJdbcTemplate().queryForObject("SELECT 1", Integer.class))
                .isInstanceOf(Exception.class);
    }

    // -------------------------------------------------------------------------
    // 带租户启动
    // -------------------------------------------------------------------------

    @Test
    void contextWithTenantEnabled() throws Exception {
        JdbcContext tenantCtx = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:ctx2;DB_CLOSE_DELAY=-1")
                .username("sa").password("")
                .poolType(PoolType.SIMPLE)
                .tenantEnabled(true)
                .build();
        // Execute DDL statements separately
        for (String stmt : DDL.split(";")) {
            String s = stmt.trim();
            if (!s.isEmpty()) {
                try (java.sql.Connection conn = tenantCtx.getConnectionProvider().getConnection();
                     java.sql.Statement st = conn.createStatement()) {
                    st.execute(s);
                }
            }
        }
        TenantContext.set("T1");
        tenantCtx.getJdbcTemplate().update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "alice", "T1");
        // 有租户拦截器，查询自动追加条件
        Long count = tenantCtx.getJdbcTemplate().queryForObject("SELECT COUNT(*) FROM app_user WHERE status = ?", Long.class, 1);
        assertThat(count).isEqualTo(1L);
        TenantContext.clear();
        tenantCtx.close();
    }

    // -------------------------------------------------------------------------
    // 命名参数 + 仓储组合使用
    // -------------------------------------------------------------------------

    @Test
    void namedAndRepositoryCoexist() {
        Repository<User, Object> repo = ctx.getRepository(User.class);
        User u1 = new User("alice", 1, null);
        repo.save(u1);
        ctx.getJdbcTemplate().update("UPDATE app_user SET name = ? WHERE id = ?", "alice_v2", u1.getId());
        List<Map<String, Object>> found = ctx.getJdbcTemplate().queryForMaps(
                "SELECT * FROM app_user WHERE id = ?", u1.getId());
        assertThat(found).hasSize(1);
        assertThat(found.get(0).get("name")).isEqualTo("alice_v2");
    }
}
