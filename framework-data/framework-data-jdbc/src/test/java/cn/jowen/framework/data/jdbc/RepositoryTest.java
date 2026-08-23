package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import cn.jowen.framework.data.jdbc.context.JdbcContextBuilder;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RepositoryTest 完整 CRUD 测试：通过 JdbcTemplate 模拟仓储行为，
 * 验证 save insert/update 自动判定、findById、findAll、existsById、count。
 */
@SuppressWarnings("unchecked")
class RepositoryTest {

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

    private JdbcContext ctx;
    private JdbcTemplate tpl;
    private BeanPropertyRowMapper<User> userMapper;
    private BeanPropertyRowMapper<Account> accountMapper;

    @BeforeEach
    void setUp() throws Exception {
        ctx = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:repo1;DB_CLOSE_DELAY=-1")
                .username("sa").password("")
                .poolType(PoolType.SIMPLE)
                .build();
        tpl = ctx.getJdbcTemplate();
        userMapper = BeanPropertyRowMapper.of(User.class);
        accountMapper = BeanPropertyRowMapper.of(Account.class);
        tpl.execute(DDL);
        // Cleanup tables to prevent data pollution
        try (java.sql.Connection conn = ctx.getConnectionProvider().getConnection();
             java.sql.Statement st = conn.createStatement()) {
            st.execute("DELETE FROM app_user");
            st.execute("DELETE FROM app_account");
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        ctx.close();
    }

    // -------------------------------------------------------------------------
    // 辅助：模拟仓储 save（insert 或 update）
    // -------------------------------------------------------------------------

    private User saveUser(User user) {
        Long id = user.getId();
        if (id == null) {
            Object key = tpl.executeInsert("INSERT INTO app_user (name, status, tenant_id) VALUES (?, ?, ?)",
                    user.getName(), user.getStatus(), user.getTenantId());
            user.setId(((Number) key).longValue());
        } else if (existsUser(id)) {
            tpl.update("UPDATE app_user SET name = ?, status = ?, tenant_id = ? WHERE id = ?",
                    user.getName(), user.getStatus(), user.getTenantId(), id);
        } else {
            tpl.executeInsert("INSERT INTO app_user (id, name, status, tenant_id) VALUES (?, ?, ?, ?)",
                    id, user.getName(), user.getStatus(), user.getTenantId());
        }
        return user;
    }

    private boolean existsUser(Long id) {
        Long cnt = tpl.queryForObject("SELECT COUNT(*) FROM app_user WHERE id = ?", Long.class, id);
        return cnt != null && cnt > 0;
    }

    private User findById(Long id) {
        List<User> list = tpl.query("SELECT * FROM app_user WHERE id = ?", userMapper, id);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<User> findAllUsers() {
        return tpl.query("SELECT * FROM app_user", userMapper);
    }

    private long countUsers() {
        Long c = tpl.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        return c == null ? 0L : c;
    }

    // -------------------------------------------------------------------------
    // save 插入新实体
    // -------------------------------------------------------------------------

    @Test
    void save_insertsNewEntity() {
        User user = new User("alice", 1, null);
        User saved = saveUser(user);
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("alice");
        Optional<User> found = Optional.ofNullable(findById(saved.getId()));
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("alice");
    }

    // -------------------------------------------------------------------------
    // save 更新已存在实体
    // -------------------------------------------------------------------------

    @Test
    void save_updatesExistingEntity() {
        User user = new User("alice", 1, null);
        User saved = saveUser(user);
        Long id = saved.getId();
        user.setName("alice_updated");
        user.setStatus(0);
        saveUser(user);
        User updated = findById(id);
        assertThat(updated.getName()).isEqualTo("alice_updated");
        assertThat(updated.getStatus()).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // findById
    // -------------------------------------------------------------------------

    @Test
    void findById_existingEntity() {
        User user = new User("carol", 1, null);
        saveUser(user);
        User found = findById(user.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("carol");
    }

    @Test
    void findById_nonExistent() {
        assertThat(findById(99999L)).isNull();
    }

    // -------------------------------------------------------------------------
    // findAll
    // -------------------------------------------------------------------------

    @Test
    void findAll_returnsAllEntities() {
        saveUser(new User("alice", 1, null));
        saveUser(new User("bob", 0, null));
        List<User> all = findAllUsers();
        assertThat(all).hasSize(2);
    }

    @Test
    void findAll_emptyWhenNoData() {
        assertThat(findAllUsers()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // existsById
    // -------------------------------------------------------------------------

    @Test
    void existsById_trueWhenPresent() {
        User user = saveUser(new User("ivan", 1, null));
        assertThat(existsUser(user.getId())).isTrue();
    }

    @Test
    void existsById_falseWhenAbsent() {
        assertThat(existsUser(99999L)).isFalse();
    }

    // -------------------------------------------------------------------------
    // count
    // -------------------------------------------------------------------------

    @Test
    void count_returnsNumberOfEntities() {
        saveUser(new User("alice", 1, null));
        saveUser(new User("bob", 1, null));
        assertThat(countUsers()).isEqualTo(2L);
    }

    @Test
    void count_zeroWhenEmpty() {
        assertThat(countUsers()).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // deleteById
    // -------------------------------------------------------------------------

    @Test
    void deleteById_removesEntity() {
        User user = saveUser(new User("eve", 1, null));
        tpl.update("DELETE FROM app_user WHERE id = ?", user.getId());
        assertThat(findById(user.getId())).isNull();
    }

    @Test
    void deleteById_nonExistent() {
        int rows = tpl.update("DELETE FROM app_user WHERE id = ?", 99999L);
        assertThat(rows).isEqualTo(0);
    }

    // -------------------------------------------------------------------------
    // deleteAll
    // -------------------------------------------------------------------------

    @Test
    void deleteAll_removesAllEntities() {
        saveUser(new User("grace", 1, null));
        saveUser(new User("hank", 1, null));
        int deleted = tpl.update("DELETE FROM app_user");
        assertThat(deleted).isEqualTo(2);
        assertThat(countUsers()).isEqualTo(0L);
    }

    // -------------------------------------------------------------------------
    // Account 实体测试
    // -------------------------------------------------------------------------

    @Test
    void account_saveAndFind() {
        Account acc = new Account("alice", new java.math.BigDecimal("100.50"));
        Object key = tpl.executeInsert("INSERT INTO app_account (owner, balance) VALUES (?, ?)",
                acc.getOwner(), acc.getBalance());
        acc.setId(((Number) key).longValue());
        List<Account> list = tpl.query("SELECT * FROM app_account WHERE id = ?", accountMapper, acc.getId());
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getOwner()).isEqualTo("alice");
        assertThat(list.get(0).getBalance()).isEqualByComparingTo(new java.math.BigDecimal("100.50"));
    }
}
