package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.transaction.TransactionCallback;
import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import cn.jowen.framework.data.jdbc.transaction.JdbcTransactionManager;
import cn.jowen.framework.data.jdbc.transaction.TransactionSynchronizationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 事务测试：编程式事务提交/回滚、REQUIRED/REQUIRES_NEW 传播行为。
 */
class TransactionTest {

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
    private JdbcTransactionManager tm;
    private ConnectionProvider connectionProvider;

    @BeforeEach
    void setUp() throws Exception {
        DataSourceProperties dsProps = new DataSourceProperties();
        dsProps.setUrl("jdbc:h2:mem:tx1;DB_CLOSE_DELAY=-1");
        dsProps.setUsername("sa");
        dsProps.setPassword("");
        dsProps.setPoolType(PoolType.SIMPLE);
        JdbcProperties props = new JdbcProperties();
        props.setSqlLogEnabled(false);
        InterceptorChain chain = new InterceptorChain();
        SimpleConnectionProvider provider = new SimpleConnectionProvider(dsProps);
        connectionProvider = provider;
        template = new JdbcTemplate(provider, chain);
        tm = new JdbcTransactionManager(provider);
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
        // 清理线程绑定（事务完成后理论上应已清理，防止污染其他测试）
        TransactionSynchronizationManager.unbindResource();
        if (connectionProvider != null) {
            connectionProvider.close();
        }
    }

    // -------------------------------------------------------------------------
    // 提交
    // -------------------------------------------------------------------------

    @Test
    void commitPersistsData() {
        TransactionDefinition def = TransactionDefinition.defaults();
        TransactionStatus status = tm.begin(def);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            tm.commit(status);
        } catch (RuntimeException e) {
            tm.rollback(status);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void rollbackDiscardsData() {
        TransactionDefinition def = TransactionDefinition.defaults();
        TransactionStatus status = tm.begin(def);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            tm.rollback(status);
        } catch (RuntimeException e) {
            tm.rollback(status);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(0L);
    }

    @Test
    void exceptionDuringTxTriggersRollback() {
        TransactionDefinition def = TransactionDefinition.defaults();
        TransactionStatus status = tm.begin(def);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            throw new RuntimeException("simulated failure");
        } catch (RuntimeException e) {
            tm.rollback(status);
            assertThat(e.getMessage()).isEqualTo("simulated failure");
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // REQUIRED 传播：内层加入外层事务
    // -------------------------------------------------------------------------

    @Test
    void required_propagationJoinsOuterTx() {
        TransactionDefinition outerDef = TransactionDefinition.defaults();
        TransactionStatus outer = tm.begin(outerDef);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            // 内层也是 REQUIRED，应加入外层事务
            TransactionStatus inner = tm.begin(TransactionDefinition.defaults());
            template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
            tm.commit(inner);
            tm.commit(outer);
        } catch (RuntimeException e) {
            tm.rollback(outer);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void required_innerRollbackPropagatesToOuter() {
        TransactionDefinition outerDef = TransactionDefinition.defaults();
        TransactionStatus outer = tm.begin(outerDef);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            TransactionStatus inner = tm.begin(TransactionDefinition.defaults());
            template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
            inner.setRollbackOnly();
            tm.commit(inner);
            tm.commit(outer);
        } catch (RuntimeException e) {
            tm.rollback(outer);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        // 内层标记回滚，整笔事务应回滚
        assertThat(count).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // REQUIRES_NEW 传播：挂起外层，新开独立事务
    // -------------------------------------------------------------------------

    @Test
    void requiresNewBeginsIndependentTx() {
        TransactionDefinition outerDef = TransactionDefinition.defaults();
        TransactionStatus outer = tm.begin(outerDef);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            // REQUIRES_NEW：挂起外层，开新事务
            TransactionDefinition newDef = new TransactionDefinition(
                    TransactionDefinition.Propagation.REQUIRES_NEW,
                    TransactionDefinition.Isolation.DEFAULT, -1, false);
            TransactionStatus inner = tm.begin(newDef);
            template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
            tm.commit(inner);
            // 外层提交（不影响内层）
            tm.commit(outer);
        } catch (RuntimeException e) {
            tm.rollback(outer);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(2L);
    }

    @Test
    void requiresNew_rollbackInnerDoesNotAffectOuter() {
        TransactionDefinition outerDef = TransactionDefinition.defaults();
        TransactionStatus outer = tm.begin(outerDef);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            TransactionDefinition newDef = new TransactionDefinition(
                    TransactionDefinition.Propagation.REQUIRES_NEW,
                    TransactionDefinition.Isolation.DEFAULT, -1, false);
            TransactionStatus inner = tm.begin(newDef);
            template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
            tm.rollback(inner);
            tm.commit(outer);
        } catch (RuntimeException e) {
            tm.rollback(outer);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        // 内层回滚，外层提交，只有 alice 保留
        assertThat(count).isEqualTo(1L);
    }

    // -------------------------------------------------------------------------
    // TransactionTemplate
    // -------------------------------------------------------------------------

    @Test
    void transactionTemplate_commitOnSuccess() {
        template.execute("DELETE FROM app_user");
        cn.jowen.framework.data.core.transaction.TransactionTemplate txTemplate =
                new cn.jowen.framework.data.core.transaction.TransactionTemplate(tm);
        txTemplate.execute((TransactionCallback) status -> {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
        });
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void transactionTemplate_rollbackOnException() {
        template.execute("DELETE FROM app_user");
        cn.jowen.framework.data.core.transaction.TransactionTemplate txTemplate =
                new cn.jowen.framework.data.core.transaction.TransactionTemplate(tm);
        assertThatThrownBy(() -> txTemplate.execute((TransactionCallback) status -> {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            throw new RuntimeException("forced failure");
        })).isInstanceOf(RuntimeException.class)
           .hasMessage("forced failure");
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // 账户转账场景（完整的事务一致性测试）
    // -------------------------------------------------------------------------

    @Test
    void transactionTemplate_transferSuccess() {
        template.execute("DELETE FROM app_account");
        template.update("INSERT INTO app_account (owner, balance) VALUES (?, ?)", "alice", 1000);
        template.update("INSERT INTO app_account (owner, balance) VALUES (?, ?)", "bob", 0);
        cn.jowen.framework.data.core.transaction.TransactionTemplate txTemplate =
                new cn.jowen.framework.data.core.transaction.TransactionTemplate(tm);
        txTemplate.execute((TransactionCallback) status -> {
            template.update("UPDATE app_account SET balance = balance - ? WHERE owner = ?", 100, "alice");
            template.update("UPDATE app_account SET balance = balance + ? WHERE owner = ?", 100, "bob");
        });
        BigDecimal aliceBal = queryBalance("alice");
        BigDecimal bobBal = queryBalance("bob");
        assertThat(aliceBal).isEqualByComparingTo(new java.math.BigDecimal("900"));
        assertThat(bobBal).isEqualByComparingTo(new java.math.BigDecimal("100"));
    }

    @Test
    void transactionTemplate_transferRollbackOnException() {
        template.execute("DELETE FROM app_account");
        template.update("INSERT INTO app_account (owner, balance) VALUES (?, ?)", "alice", 1000);
        template.update("INSERT INTO app_account (owner, balance) VALUES (?, ?)", "bob", 0);
        cn.jowen.framework.data.core.transaction.TransactionTemplate txTemplate =
                new cn.jowen.framework.data.core.transaction.TransactionTemplate(tm);
        assertThatThrownBy(() -> txTemplate.execute((TransactionCallback) status -> {
            template.update("UPDATE app_account SET balance = balance - ? WHERE owner = ?", 100, "alice");
            throw new RuntimeException("transfer failed");
        })).isInstanceOf(RuntimeException.class);
        BigDecimal aliceBal = queryBalance("alice");
        BigDecimal bobBal = queryBalance("bob");
        assertThat(aliceBal).isEqualByComparingTo(new java.math.BigDecimal("1000"));
        assertThat(bobBal).isEqualByComparingTo(new java.math.BigDecimal("0"));
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    private java.math.BigDecimal queryBalance(String owner) {
        Object bal = template.queryForObject(
                "SELECT balance FROM app_account WHERE owner = ?", Object.class, owner);
        return new java.math.BigDecimal(bal.toString());
    }
}
