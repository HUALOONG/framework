package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import cn.jowen.framework.data.jdbc.context.JdbcContextBuilder;
import cn.jowen.framework.data.jdbc.core.JdbcOperations;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.LoggingInterceptor;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import cn.jowen.framework.data.jdbc.mapping.MapRowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JdbcTemplate 基础 CRUD 测试：query / queryForObject / queryForMaps / execute / executeInsert / update。
 */
class JdbcTemplateTest {

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

    private JdbcTemplate template;
    private JdbcOperations rawOps;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() throws Exception {
        JdbcProperties props = new JdbcProperties();
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(new LoggingInterceptor(props));
        cn.jowen.framework.data.core.datasource.DataSourceProperties dsProps =
                new cn.jowen.framework.data.core.datasource.DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:tpl1;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(cn.jowen.framework.data.core.datasource.PoolType.SIMPLE);
        SimpleConnectionProvider provider = new SimpleConnectionProvider(dsProps);
        connectionProvider = provider;
        template = new JdbcTemplate(provider, chain);
        rawOps = template;
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
            st.execute("DROP TABLE IF EXISTS test_execute");
            st.execute("DROP TABLE IF EXISTS app_user");
            st.execute("DROP TABLE IF EXISTS app_account");
            for (String stmt : DDL.split(";")) {
                String s = stmt.trim();
                if (!s.isEmpty()) {
                    st.execute(s);
                }
            }
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
    // query
    // -------------------------------------------------------------------------

    @Test
    void query_returnsMappedEntities() {
        insertUser("alice");
        List<User> users = template.query("SELECT * FROM app_user", BeanPropertyRowMapper.of(User.class));
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("alice");
    }

    @Test
    void query_returnsEmptyWhenNoRows() {
        List<User> users = template.query("SELECT * FROM app_user", BeanPropertyRowMapper.of(User.class));
        assertThat(users).isEmpty();
    }

    @Test
    void query_withParams() {
        insertUser("bob");
        insertUser("carol");
        List<User> users = template.query("SELECT * FROM app_user WHERE name = ?",
                BeanPropertyRowMapper.of(User.class), "bob");
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("bob");
    }

    // -------------------------------------------------------------------------
    // queryForObject (RowMapper)
    // -------------------------------------------------------------------------

    @Test
    void queryForObject_returnsSingleEntity() {
        Long id = insertUser("dave");
        User one = template.queryForObject("SELECT * FROM app_user WHERE id = ?",
                BeanPropertyRowMapper.of(User.class), id);
        assertThat(one).isNotNull();
        assertThat(one.getName()).isEqualTo("dave");
    }

    @Test
    void queryForObject_throwsWhenEmpty() {
        assertThatThrownBy(() ->
                template.queryForObject("SELECT * FROM app_user WHERE id = ?",
                        BeanPropertyRowMapper.of(User.class), 999L))
                .isInstanceOf(cn.jowen.framework.data.core.exception.DataAccessException.class);
    }

    // -------------------------------------------------------------------------
    // queryForObject (scalar)
    // -------------------------------------------------------------------------

    @Test
    void queryForObject_scalar_long() {
        insertUser("eve");
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void queryForObject_scalar_string() {
        insertUser("frank");
        String name = template.queryForObject("SELECT name FROM app_user WHERE id = ?", String.class, 1L);
        assertThat(name).isEqualTo("frank");
    }

    // -------------------------------------------------------------------------
    // queryForList
    // -------------------------------------------------------------------------

    @Test
    void queryForList_returnsScalarList() {
        insertUser("grace");
        insertUser("hank");
        List<String> names = template.queryForList("SELECT name FROM app_user", String.class);
        assertThat(names).containsExactlyInAnyOrder("grace", "hank");
    }

    // -------------------------------------------------------------------------
    // queryForMaps
    // -------------------------------------------------------------------------

    @Test
    void queryForMaps_returnsRawMaps() {
        insertUser("ivan");
        List<Map<String, Object>> maps = template.queryForMaps("SELECT * FROM app_user WHERE name = ?", "ivan");
        assertThat(maps).hasSize(1);
        Map<String, Object> row = maps.get(0);
        assertThat(row).containsEntry("name", "ivan");
        assertThat(row.get("id")).isNotNull();
    }

    @Test
    void queryForMaps_emptyResult() {
        List<Map<String, Object>> maps = template.queryForMaps("SELECT * FROM app_user WHERE id = ?", 999L);
        assertThat(maps).isEmpty();
    }

    // -------------------------------------------------------------------------
    // execute
    // -------------------------------------------------------------------------

    @Test
    void execute_createsTableAndDrops() {
        template.execute("CREATE TABLE test_execute (id BIGINT PRIMARY KEY)");
        // table exists — verify by inserting
        template.update("INSERT INTO test_execute (id) VALUES (?)", 1L);
        Long count = template.queryForObject("SELECT COUNT(*) FROM test_execute", Long.class);
        assertThat(count).isEqualTo(1L);
        template.execute("DROP TABLE test_execute");
    }

    // -------------------------------------------------------------------------
    // update
    // -------------------------------------------------------------------------

    @Test
    void update_insertsRow() {
        int rows = template.update("INSERT INTO app_user (name) VALUES (?)", "jack");
        assertThat(rows).isEqualTo(1);
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void update_updatesRow() {
        Long id = insertUser("kevin");
        int rows = template.update("UPDATE app_user SET name = ? WHERE id = ?", "kevin_updated", id);
        assertThat(rows).isEqualTo(1);
        String name = template.queryForObject("SELECT name FROM app_user WHERE id = ?", String.class, id);
        assertThat(name).isEqualTo("kevin_updated");
    }

    @Test
    void update_deletesRow() {
        Long id = insertUser("larry");
        int rows = template.update("DELETE FROM app_user WHERE id = ?", id);
        assertThat(rows).isEqualTo(1);
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // executeInsert
    // -------------------------------------------------------------------------

    @Test
    void executeInsert_returnsGeneratedKey() {
        Object key = template.executeInsert("INSERT INTO app_user (name) VALUES (?)", "mike");
        assertThat(key).isNotNull();
        Long id = ((Number) key).longValue();
        User found = template.queryForObject("SELECT * FROM app_user WHERE id = ?",
                BeanPropertyRowMapper.of(User.class), id);
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("mike");
    }

    @Test
    void executeInsert_userWithAllFields() {
        Object key = template.executeInsert(
                "INSERT INTO app_user (name, status, tenant_id) VALUES (?, ?, ?)",
                "nancy", 0, "T1");
        Long id = ((Number) key).longValue();
        User user = template.queryForObject("SELECT * FROM app_user WHERE id = ?",
                BeanPropertyRowMapper.of(User.class), id);
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("nancy");
        assertThat(user.getStatus()).isEqualTo(0);
        assertThat(user.getTenantId()).isEqualTo("T1");
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    private Long insertUser(String name) {
        template.update("INSERT INTO app_user (name) VALUES (?)", name);
        return template.queryForObject("SELECT MAX(id) FROM app_user", Long.class);
    }

    @Test
    void insertAccount_withBigDecimal() {
        template.executeInsert("INSERT INTO app_account (owner, balance) VALUES (?, ?)", "alice", new BigDecimal("100.50"));
        List<Account> accounts = template.query("SELECT * FROM app_account", BeanPropertyRowMapper.of(Account.class));
        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).getBalance()).isEqualByComparingTo(new BigDecimal("100.50"));
    }

    // -------------------------------------------------------------------------
    // batchUpdate
    // -------------------------------------------------------------------------

    @Test
    void batchUpdate_multipleStatements() {
        int[] r = template.batchUpdate(
                "INSERT INTO app_user (name) VALUES ('b1')",
                "INSERT INTO app_user (name) VALUES ('b2')");
        assertThat(r).hasSize(2);
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isGreaterThanOrEqualTo(2L);
    }

    @Test
    void batchUpdate_withBatchArgs() {
        int[] r = template.batchUpdate(
                "INSERT INTO app_user (name) VALUES (?)",
                List.of(new Object[]{"c1"}, new Object[]{"c2"}));
        assertThat(r).hasSize(2);
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isGreaterThanOrEqualTo(2L);
    }

    @Test
    void query_invalidTable_throwsDataAccessException() {
        assertThatThrownBy(() -> template.queryForMaps("SELECT * FROM no_such_table_xyz", 1L))
                .isInstanceOf(cn.jowen.framework.data.core.exception.DataAccessException.class);
    }
}
