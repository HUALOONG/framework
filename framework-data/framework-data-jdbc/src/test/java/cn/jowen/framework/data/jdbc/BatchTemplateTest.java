package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.core.BatchTemplate;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BatchTemplate 批量插入/更新测试。
 */
class BatchTemplateTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            CREATE TABLE IF NOT EXISTS app_account (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                owner VARCHAR(50) NOT NULL,
                balance DECIMAL(19,2) DEFAULT 0
            );
            """;

    private BatchTemplate batch;
    private JdbcTemplate template;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:batch1;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        SimpleConnectionProvider provider = new SimpleConnectionProvider(dsProps);
        connectionProvider = provider;
        template = new JdbcTemplate(provider, chain);
        template = new JdbcTemplate(new SimpleConnectionProvider(dsProps), chain);
        batch = new BatchTemplate(template);
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
    // batchUpdate (varargs SQL)
    // -------------------------------------------------------------------------

    @Test
    void batchUpdate_multipleSqlStatements() {
        int[] results = template.batchUpdate(
                "INSERT INTO app_user (name) VALUES ('alice')",
                "INSERT INTO app_user (name) VALUES ('bob')",
                "INSERT INTO app_user (name) VALUES ('carol')");
        assertThat(results).hasSize(3);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
        assertThat(results[2]).isEqualTo(1);
    }

    @Test
    void batchUpdate_mixedInsertAndDelete() {
        template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
        template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
        int[] results = template.batchUpdate(
                "DELETE FROM app_user WHERE name = 'alice'",
                "DELETE FROM app_user WHERE name = 'bob'");
        assertThat(results).hasSize(2);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
    }

    // -------------------------------------------------------------------------
    // batchUpdate (parameterized with List<Object[]>)
    // -------------------------------------------------------------------------

    @Test
    void batchUpdate_parameterized() {
        int[] results = template.batchUpdate(
                "INSERT INTO app_user (name) VALUES (?)",
                List.of(new Object[]{"alice"}, new Object[]{"bob"}, new Object[]{"carol"}));
        assertThat(results).hasSize(3);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
        assertThat(results[2]).isEqualTo(1);
    }

    @Test
    void batchUpdate_parameterized_emptyArgs() {
        int[] results = template.batchUpdate("INSERT INTO app_user (name) VALUES (?)", List.of());
        assertThat(results).isEmpty();
    }

    // -------------------------------------------------------------------------
    // BatchTemplate API
    // -------------------------------------------------------------------------

    @Test
    void batchTemplate_addBatchAndExecute() {
        int[] results = batch
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "alice")
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "bob")
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "carol")
                .executeBatch();
        assertThat(results).hasSize(3);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
        assertThat(results[2]).isEqualTo(1);
    }

    @Test
    void batchTemplate_clearResetsState() {
        batch.addBatch("INSERT INTO app_user (name) VALUES (?)", "alice");
        batch.clear();
        int[] results = batch.executeBatch();
        assertThat(results).isEmpty();
    }

    @Test
    void batchTemplate_emptyExecuteReturnsEmpty() {
        int[] results = batch.executeBatch();
        assertThat(results).isEmpty();
    }

    @Test
    void batchTemplate_groupsBySql() {
        // Same SQL template with different params should be grouped together
        int[] results = batch
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "alice")
                .addBatch("INSERT INTO app_user (name) VALUES (?)", "bob")
                .executeBatch();
        assertThat(results).hasSize(2);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
    }

    @Test
    void batchTemplate_addBatchWithListParams() {
        int[] results = batch
                .addBatch("INSERT INTO app_account (owner, balance) VALUES (?, ?)",
                        List.of("alice", 100.50))
                .addBatch("INSERT INTO app_account (owner, balance) VALUES (?, ?)",
                        List.of("bob", 200.00))
                .executeBatch();
        assertThat(results).hasSize(2);
        assertThat(results[0]).isEqualTo(1);
        assertThat(results[1]).isEqualTo(1);
    }

    @Test
    void batchTemplate_varietyOfOps() {
        // Insert multiple users
        batch.addBatch("INSERT INTO app_user (name) VALUES (?)", "alice");
        batch.addBatch("INSERT INTO app_user (name) VALUES (?)", "bob");
        // Update an existing user
        batch.addBatch("UPDATE app_user SET name = ? WHERE name = ?", "alice_v2", "alice");
        int[] results = batch.executeBatch();
        assertThat(results).hasSize(3);
        // Verify update took effect
        long count = template.queryForObject("SELECT COUNT(*) FROM app_user WHERE name = 'alice_v2'", Long.class);
        assertThat(count).isEqualTo(1L);
    }
}
