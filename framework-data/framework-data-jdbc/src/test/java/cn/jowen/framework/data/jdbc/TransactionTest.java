package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.transaction.Isolation;
import cn.jowen.framework.data.core.transaction.Propagation;
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
 * 浜嬪姟娴嬭瘯锛氱紪绋嬪紡浜嬪姟鎻愪氦/鍥炴粴銆丷EQUIRED/REQUIRES_NEW 浼犳挱琛屼负銆?
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
        // 娓呯悊绾跨▼缁戝畾锛堜簨鍔″畬鎴愬悗鐞嗚涓婂簲宸叉竻鐞嗭紝闃叉姹℃煋鍏朵粬娴嬭瘯锛?
        TransactionSynchronizationManager.unbindResource();
        if (connectionProvider != null) {
            connectionProvider.close();
        }
    }

    // -------------------------------------------------------------------------
    // 鎻愪氦
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
    // REQUIRED 浼犳挱锛氬唴灞傚姞鍏ュ灞備簨鍔?
    // -------------------------------------------------------------------------

    @Test
    void required_propagationJoinsOuterTx() {
        TransactionDefinition outerDef = TransactionDefinition.defaults();
        TransactionStatus outer = tm.begin(outerDef);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            // 鍐呭眰涔熸槸 REQUIRED锛屽簲鍔犲叆澶栧眰浜嬪姟
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
        // 鍐呭眰鏍囪鍥炴粴锛屾暣绗斾簨鍔″簲鍥炴粴
        assertThat(count).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // REQUIRES_NEW 浼犳挱锛氭寕璧峰灞傦紝鏂板紑鐙珛浜嬪姟
    // -------------------------------------------------------------------------

    @Test
    void requiresNewBeginsIndependentTx() {
        TransactionDefinition outerDef = TransactionDefinition.defaults();
        TransactionStatus outer = tm.begin(outerDef);
        try {
            template.update("INSERT INTO app_user (name) VALUES (?)", "alice");
            // REQUIRES_NEW锛氭寕璧峰灞傦紝寮€鏂颁簨鍔?
            TransactionDefinition newDef = new TransactionDefinition(
                    Propagation.REQUIRES_NEW,
                    Isolation.DEFAULT, -1, false);
            TransactionStatus inner = tm.begin(newDef);
            template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
            tm.commit(inner);
            // 澶栧眰鎻愪氦锛堜笉褰卞搷鍐呭眰锛?
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
                    Propagation.REQUIRES_NEW,
                    Isolation.DEFAULT, -1, false);
            TransactionStatus inner = tm.begin(newDef);
            template.update("INSERT INTO app_user (name) VALUES (?)", "bob");
            tm.rollback(inner);
            tm.commit(outer);
        } catch (RuntimeException e) {
            tm.rollback(outer);
            throw e;
        }
        Long count = template.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        // 鍐呭眰鍥炴粴锛屽灞傛彁浜わ紝鍙湁 alice 淇濈暀
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
    // 璐︽埛杞处鍦烘櫙锛堝畬鏁寸殑浜嬪姟涓€鑷存€ф祴璇曪級
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
    // 杈呭姪鏂规硶
    // -------------------------------------------------------------------------

    private java.math.BigDecimal queryBalance(String owner) {
        Object bal = template.queryForObject(
                "SELECT balance FROM app_account WHERE owner = ?", Object.class, owner);
        return new java.math.BigDecimal(bal.toString());
    }
}

