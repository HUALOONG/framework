package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.mapping.RowMapper;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.core.NamedParameterTemplate;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NamedParameterTemplate 命名参数查询/更新测试。
 */
class NamedParameterTemplateTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            """;

    private NamedParameterTemplate named;
    private JdbcTemplate template;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:named1;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties jdbcProps = new JdbcProperties();
        jdbcProps.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        SimpleConnectionProvider provider = new SimpleConnectionProvider(dsProps);
        connectionProvider = provider;
        template = new JdbcTemplate(provider, chain);
        named = new NamedParameterTemplate(template);
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
    // query with Map params
    // -------------------------------------------------------------------------

    @Test
    void queryWithMapParams() {
        template.update("INSERT INTO app_user (name, status) VALUES (?, ?)", "alice", 1);
        List<User> users = named.query("SELECT * FROM app_user WHERE name = :name",
                BeanPropertyRowMapper.of(User.class), Map.of("name", "alice"));
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getName()).isEqualTo("alice");
    }

    @Test
    void queryWithMapParams_multipleNamed() {
        template.update("INSERT INTO app_user (name, status) VALUES (?, ?)", "bob", 0);
        User user = named.queryForObject("SELECT * FROM app_user WHERE name = :name AND status = :status",
                BeanPropertyRowMapper.of(User.class), Map.of("name", "bob", "status", 0));
        assertThat(user).isNotNull();
        assertThat(user.getStatus()).isEqualTo(0);
    }

    @Test
    void queryWithMapParams_emptyResult() {
        List<User> users = named.query("SELECT * FROM app_user WHERE name = :name",
                BeanPropertyRowMapper.of(User.class), Map.of("name", "nonexist"));
        assertThat(users).isEmpty();
    }

    // -------------------------------------------------------------------------
    // queryForObject with Map params
    // -------------------------------------------------------------------------

    @Test
    void queryForObjectWithMapParams() {
        template.update("INSERT INTO app_user (name) VALUES (?)", "carol");
        User user = named.queryForObject("SELECT * FROM app_user WHERE name = :name",
                BeanPropertyRowMapper.of(User.class), Map.of("name", "carol"));
        assertThat(user).isNotNull();
        assertThat(user.getName()).isEqualTo("carol");
    }

    // -------------------------------------------------------------------------
    // update with Map params
    // -------------------------------------------------------------------------

    @Test
    void updateWithMapParams() {
        Long id = insertUser("dave");
        int rows = named.update("UPDATE app_user SET name = :name WHERE id = :id",
                Map.of("name", "dave_updated", "id", id));
        assertThat(rows).isEqualTo(1);
        String name = template.queryForObject("SELECT name FROM app_user WHERE id = ?", String.class, id);
        assertThat(name).isEqualTo("dave_updated");
    }

    // -------------------------------------------------------------------------
    // query with Bean params
    // -------------------------------------------------------------------------

    @Test
    void queryWithBeanParams() {
        template.update("INSERT INTO app_user (name, status) VALUES (?, ?)", "eve", 1);
        User param = new User();
        param.setName("eve");
        param.setStatus(1);
        List<User> users = named.query("SELECT * FROM app_user WHERE name = :name AND status = :status",
                BeanPropertyRowMapper.of(User.class), param);
        assertThat(users).hasSize(1);
    }

    @Test
    void updateWithBeanParams() {
        Long id = insertUser("frank");
        User param = new User();
        param.setId(id);
        param.setName("frank_updated");
        int rows = named.update("UPDATE app_user SET name = :name WHERE id = :id", param);
        assertThat(rows).isEqualTo(1);
        String name = template.queryForObject("SELECT name FROM app_user WHERE id = ?", String.class, id);
        assertThat(name).isEqualTo("frank_updated");
    }

    // -------------------------------------------------------------------------
    // error cases
    // -------------------------------------------------------------------------

    @Test
    void queryMissingParamThrows() {
        assertThatThrownBy(() ->
                named.query("SELECT * FROM app_user WHERE name = :name",
                        BeanPropertyRowMapper.of(User.class), Map.of("other", "x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("命名参数缺失");
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    private Long insertUser(String name) {
        template.update("INSERT INTO app_user (name) VALUES (?)", name);
        return template.queryForObject("SELECT MAX(id) FROM app_user", Long.class);
    }
}
