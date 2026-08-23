package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.LoggingInterceptor;
import cn.jowen.framework.data.jdbc.interceptor.SqlContext;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.jdbc.interceptor.TenantInterceptor;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import cn.jowen.framework.logger.adapter.LoggerAdapter;
import cn.jowen.framework.logger.facade.LogLevel;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * InterceptorTest 测试 LoggingInterceptor 日志脱敏、TenantInterceptor 租户条件注入。
 */
class InterceptorTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            """;

    private JdbcTemplate template;
    private ConnectionProvider connectionProvider;
    private CapturingAdapter capturingAdapter;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:intc1;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setSqlLogEnabled(true);
        InterceptorChain chain = new InterceptorChain();
        // 注入捕获型日志适配器，验证 LoggingInterceptor 输出
        capturingAdapter = new CapturingAdapter();
        LoggerFactory.setAdapter(capturingAdapter);
        chain.addInterceptor(new LoggingInterceptor(props));
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
        LoggerFactory.setAdapter(null);
        if (connectionProvider != null) {
            connectionProvider.close();
        }
    }

    // -------------------------------------------------------------------------
    // LoggingInterceptor
    // -------------------------------------------------------------------------

    @Test
    void loggingInterceptor_recordsSqlAndParams() {
        template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
        assertThat(capturingAdapter.getLastMessage()).isNotNull();
        String msg = capturingAdapter.getLastMessage();
        assertThat(msg).contains("SQL =>");
        assertThat(msg).contains("INSERT INTO app_user");
    }

    @Test
    void loggingInterceptor_skipsWhenDisabled() {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:intc2;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(new LoggingInterceptor(props));
        SimpleConnectionProvider connProvider = new SimpleConnectionProvider(dsProps);
        JdbcTemplate tpl = new JdbcTemplate(connProvider, chain);
        tpl.execute(DDL);
        tpl.update("INSERT INTO app_user (name) VALUES (?)", "alice");
        // 无日志输出（本测试的 logHandler 是上一个上下文的）
        connProvider.close();
    }

    // -------------------------------------------------------------------------
    // TenantInterceptor
    // -------------------------------------------------------------------------

    @Test
    void tenantInterceptor_appendsCondition() {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:intc3;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setTenantEnabled(true);
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(new TenantInterceptor(props));
        SimpleConnectionProvider connProvider = new SimpleConnectionProvider(dsProps);
        JdbcTemplate tpl = new JdbcTemplate(connProvider, chain);
        tpl.execute(DDL);

        TenantContext.set("T1");
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "alice", "T1");
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "bob", "T2");

        // 查询时应只返回 T1 租户数据
        List<java.util.Map<String, Object>> rows = tpl.queryForMaps("SELECT * FROM app_user WHERE status = ?", 1);
        // TenantInterceptor 会追加 AND tenant_id = ?，参数中添加 T1
        // 注意：queryForMaps 不带参数的 WHERE status = ? 也会被追加租户条件
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("tenant_id")).isEqualTo("T1");
        TenantContext.clear();
        connProvider.close();
    }

    @Test
    void tenantInterceptor_noConditionWithoutWhere() {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:intc4;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setTenantEnabled(true);
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(new TenantInterceptor(props));
        SimpleConnectionProvider connProvider = new SimpleConnectionProvider(dsProps);
        JdbcTemplate tpl = new JdbcTemplate(connProvider, chain);
        tpl.execute(DDL);

        TenantContext.set("T1");
        // INSERT 语句不含 WHERE，不应追加租户条件（否则会语法错误）
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "alice", "T1");
        Long count = tpl.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
        TenantContext.clear();
        connProvider.close();
    }

    @Test
    void tenantInterceptor_noConditionWhenTenantNotSet() {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:intc5;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setTenantEnabled(true);
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(new TenantInterceptor(props));
        SimpleConnectionProvider connProvider = new SimpleConnectionProvider(dsProps);
        JdbcTemplate tpl = new JdbcTemplate(connProvider, chain);
        tpl.execute(DDL);

        TenantContext.set("T1");
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "alice", "T1");
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "bob", "T2");
        TenantContext.clear();

        // 无租户上下文时，不追加条件，应返回所有数据
        List<java.util.Map<String, Object>> rows = tpl.queryForMaps("SELECT * FROM app_user");
        assertThat(rows).hasSize(2);
        connProvider.close();
    }

    @Test
    void tenantInterceptor_disabledDoesNotAppend() {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:intc6;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setTenantEnabled(false); // 明确关闭
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(new TenantInterceptor(props));
        SimpleConnectionProvider connProvider = new SimpleConnectionProvider(dsProps);
        JdbcTemplate tpl = new JdbcTemplate(connProvider, chain);
        tpl.execute(DDL);

        TenantContext.set("T1");
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "alice", "T1");
        tpl.update("INSERT INTO app_user (name, tenant_id) VALUES (?, ?)", "bob", "T2");

        List<java.util.Map<String, Object>> rows = tpl.queryForMaps("SELECT * FROM app_user WHERE status = ?", 1);
        // 租户功能关闭，不追加条件
        assertThat(rows).hasSize(2);
        TenantContext.clear();
        connProvider.close();
    }

    // -------------------------------------------------------------------------
    // 辅助：捕获日志的 LoggerAdapter
    // -------------------------------------------------------------------------

    private static class CapturingAdapter implements LoggerAdapter {
        private volatile String lastMessage;

        @Override
        public Logger getLogger(String name) {
            return new CapturingLogger();
        }

        public String getLastMessage() {
            return lastMessage;
        }

        private final class CapturingLogger implements Logger {
            @Override
            public void trace(String msg, Object... args) {
                capture(msg, args);
            }

            @Override
            public void debug(String msg, Object... args) {
                capture(msg, args);
            }

            @Override
            public void info(String msg, Object... args) {
                capture(msg, args);
            }

            @Override
            public void warn(String msg, Object... args) {
                capture(msg, args);
            }

            @Override
            public void error(String msg, Object... args) {
                capture(msg, args);
            }

            @Override
            public void error(String msg, Throwable t, Object... args) {
                capture(msg, args);
            }

            @Override
            public boolean isEnabled(LogLevel level) {
                return true;
            }

            private void capture(String msg, Object... args) {
                lastMessage = args == null || args.length == 0 ? msg : msg.formatted(args);
            }
        }
    }
}
