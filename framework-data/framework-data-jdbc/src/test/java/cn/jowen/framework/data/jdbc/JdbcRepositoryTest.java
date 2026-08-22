package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.page.Sort;
import cn.jowen.framework.data.core.query.Query;
import cn.jowen.framework.data.core.repository.Repository;
import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.jdbc.dialect.StandardDialect;
import cn.jowen.framework.data.jdbc.mapping.DefaultEntityMetadataResolver;
import cn.jowen.framework.data.jdbc.repository.JdbcRepository;
import cn.jowen.framework.data.jdbc.repository.JdbcRepositoryFactory;
import cn.jowen.framework.data.jdbc.transaction.JdbcTransactionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2 出口验证：JDBC CRUD + 事务 + 分页，经 H2 内存库跑通。
 */
class JdbcRepositoryTest {

    private JdbcRepository<User, Long> repository;
    private TransactionManager txManager;

    @BeforeEach
    void setUp() {
        SimpleDriverDataSource ds = new SimpleDriverDataSource();
        ds.setDriverClass(org.h2.Driver.class);
        ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("");

        JdbcRepositoryFactory factory = new JdbcRepositoryFactory(ds, new DefaultEntityMetadataResolver(), new StandardDialect());
        repository = (JdbcRepository<User, Long>) factory.<User, Long>getRepository(User.class);
        txManager = new JdbcTransactionManager(ds);

        factory.getJdbcTemplate().execute(
                "CREATE TABLE IF NOT EXISTS t_user (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(64), age INT, email VARCHAR(128))");
        factory.getJdbcTemplate().execute("DELETE FROM t_user");
    }

    @Test
    void crudAndPageAndTransaction() {
        // 1) 插入
        User u1 = repository.save(new User("Alice", 20, "alice@example.com"));
        User u2 = repository.save(new User("Bob", 25, "bob@example.com"));
        User u3 = repository.save(new User("Carol", 30, "carol@example.com"));
        assertTrue(u1.getId() != null, "插入后应回填主键");

        // 2) 查询
        Optional<User> found = repository.findById(u2.getId());
        assertTrue(found.isPresent());
        assertEquals("Bob", found.get().getName());

        // 3) 条件查询 + 脱敏字段
        List<User> adults = repository.findBy(Query.empty().gt("age", 22));
        assertEquals(2, adults.size());

        // 4) 分页（按 age 降序）
        Page<User> page = repository.findAll(Pageable.of(0, 2, Sort.by("age", Sort.Direction.DESC)));
        assertEquals(3, page.getTotal());
        assertEquals(2, page.getContent().size());
        assertEquals("Carol", page.getContent().get(0).getName());
        assertEquals(2, page.getTotalPages());

        // 5) 更新
        u1.setAge(21);
        repository.save(u1);
        assertEquals(21, repository.findById(u1.getId()).get().getAge());

        // 6) 事务回滚
        TransactionStatus tx = txManager.begin(TransactionDefinition.defaults());
        try {
            repository.save(new User("Dave", 40, "dave@example.com"));
            assertEquals(4, repository.count());
            tx.setRollbackOnly();
        } finally {
            if (tx.isRollbackOnly()) {
                txManager.rollback(tx);
            } else {
                txManager.commit(tx);
            }
        }
        assertEquals(3, repository.count(), "回滚后记录数应回到 3");

        // 7) 删除
        repository.deleteById(u3.getId());
        assertFalse(repository.existsById(u3.getId()));
        assertEquals(2, repository.count());
    }

    @Test
    void countAndQuery() {
        repository.save(new User("Eve", 18, "eve@example.com"));
        long c = repository.countBy(Query.empty().eq("name", "Eve"));
        assertEquals(1, c);
    }
}
