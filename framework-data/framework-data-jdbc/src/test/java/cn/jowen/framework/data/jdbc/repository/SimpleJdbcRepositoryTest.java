package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import cn.jowen.framework.data.core.meta.Id;
import cn.jowen.framework.data.core.meta.Table;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.PageRequest;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import cn.jowen.framework.data.jdbc.context.JdbcContextBuilder;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SimpleJdbcRepository} 集成测试：真实 H2 验证 CRUD、save 生成策略、
 * upsert 回退、分页与异常分支。
 */
class SimpleJdbcRepositoryTest {

    private static final String DDL = """
            DROP TABLE IF EXISTS app_seq;
            DROP TABLE IF EXISTS app_uuid;
            DROP TABLE IF EXISTS app_assigned;
            DROP TABLE IF EXISTS app_only_id;
            DROP TABLE IF EXISTS app_no_id;
            DROP TABLE IF EXISTS app_user;
            CREATE TABLE app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            CREATE TABLE IF NOT EXISTS app_no_id (
                name VARCHAR(50) NOT NULL
            );
            CREATE TABLE IF NOT EXISTS app_only_id (
                id BIGINT PRIMARY KEY
            );
            CREATE TABLE IF NOT EXISTS app_assigned (
                id BIGINT PRIMARY KEY,
                name VARCHAR(50)
            );
            CREATE TABLE IF NOT EXISTS app_uuid (
                id VARCHAR(36) PRIMARY KEY,
                name VARCHAR(50)
            );
            CREATE TABLE app_seq (
                id BIGINT PRIMARY KEY,
                name VARCHAR(50)
            );
            """;

    private JdbcContext ctx;
    private JdbcTemplate tpl;
    private DialectRegistry dialectRegistry;
    private EntityMetadataResolver resolver;
    private SimpleJdbcRepository<AppUser, Long> userRepo;
    private SimpleJdbcRepository<NoIdEntity, Long> noIdRepo;
    private SimpleJdbcRepository<OnlyIdEntity, Long> onlyIdRepo;
    private SimpleJdbcRepository<AssignedEntity, Long> assignedRepo;
    private SimpleJdbcRepository<UuidEntity, String> uuidRepo;
    private SimpleJdbcRepository<SeqEntity, Long> seqRepo;

    @BeforeEach
    void setUp() throws Exception {
        ctx = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:repo2;DB_CLOSE_DELAY=-1")
                .username("sa").password("")
                .poolType(PoolType.SIMPLE)
                .build();
        tpl = ctx.getJdbcTemplate();
        dialectRegistry = ctx.getDialectRegistry();
        resolver = ctx.getEntityMetadataResolver();
        tpl.execute(DDL);

        userRepo = new SimpleJdbcRepository<>(tpl, resolver, dialectRegistry,
                new DefaultIdGenerator(DefaultIdGenerator.Strategy.AUTO), AppUser.class);
        noIdRepo = new SimpleJdbcRepository<>(tpl, resolver, dialectRegistry,
                new DefaultIdGenerator(), NoIdEntity.class);
        onlyIdRepo = new SimpleJdbcRepository<>(tpl, resolver, dialectRegistry,
                new DefaultIdGenerator(), OnlyIdEntity.class);
        assignedRepo = new SimpleJdbcRepository<>(tpl, resolver, dialectRegistry,
                new DefaultIdGenerator(), AssignedEntity.class);
        uuidRepo = new SimpleJdbcRepository<>(tpl, resolver, dialectRegistry,
                new DefaultIdGenerator(DefaultIdGenerator.Strategy.UUID), UuidEntity.class);
        seqRepo = new SimpleJdbcRepository<>(tpl, resolver, dialectRegistry,
                new DefaultIdGenerator(DefaultIdGenerator.Strategy.SEQUENCE), SeqEntity.class);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        ctx.close();
    }

    // ===================== CRUD =====================

    @Test
    void save_autoGeneratesId_andFindById() {
        AppUser u = new AppUser(null, "tom", 1);
        userRepo.save(u);

        assertThat(u.getId()).isNotNull();
        Optional<AppUser> found = userRepo.findById(u.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("tom");
    }

    @Test
    void findById_missing_returnsEmpty() {
        assertThat(userRepo.findById(999L)).isEmpty();
    }

    @Test
    void findAll_returnsAll() {
        userRepo.save(new AppUser(null, "a", 1));
        userRepo.save(new AppUser(null, "b", 2));
        assertThat(userRepo.findAll()).hasSize(2);
    }

    @Test
    void existsById() {
        AppUser u = new AppUser(null, "tom", 1);
        userRepo.save(u);
        assertThat(userRepo.existsById(u.getId())).isTrue();
        assertThat(userRepo.existsById(999L)).isFalse();
    }

    @Test
    void count() {
        userRepo.save(new AppUser(null, "a", 1));
        userRepo.save(new AppUser(null, "b", 2));
        assertThat(userRepo.count()).isEqualTo(2L);
    }

    @Test
    void update_modifiesEntity() {
        AppUser u = new AppUser(null, "tom", 1);
        userRepo.save(u);
        u.setName("tom2");
        u.setStatus(5);
        userRepo.update(u);

        AppUser reloaded = userRepo.findById(u.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("tom2");
        assertThat(reloaded.getStatus()).isEqualTo(5);
    }

    @Test
    void update_noIdEntity_throws() {
        assertThatThrownBy(() -> noIdRepo.update(new NoIdEntity("x")))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无主键");
    }

    @Test
    void update_onlyIdEntity_throwsNoSettableFields() {
        OnlyIdEntity e = new OnlyIdEntity(1L);
        assertThatThrownBy(() -> onlyIdRepo.update(e))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无可更新字段");
    }

    @Test
    void deleteById_and_delete() {
        AppUser u = new AppUser(null, "tom", 1);
        userRepo.save(u);
        assertThat(userRepo.deleteById(u.getId())).isEqualTo(1);
        assertThat(userRepo.findById(u.getId())).isEmpty();

        AppUser u2 = new AppUser(null, "a", 1);
        userRepo.save(u2);
        assertThat(userRepo.delete(u2)).isEqualTo(1);
    }

    @Test
    void delete_noIdEntity_throws() {
        assertThatThrownBy(() -> noIdRepo.delete(new NoIdEntity("x")))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无主键");
    }

    @Test
    void findById_noIdEntity_throws() {
        assertThatThrownBy(() -> noIdRepo.findById(1L))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无主键列");
    }

    @Test
    void deleteById_noIdEntity_throws() {
        assertThatThrownBy(() -> noIdRepo.deleteById(1L))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无主键列");
    }

    @Test
    void deleteAll_clearsTable() {
        userRepo.save(new AppUser(null, "a", 1));
        userRepo.save(new AppUser(null, "b", 2));
        assertThat(userRepo.deleteAll()).isEqualTo(2);
        assertThat(userRepo.count()).isZero();
    }

    @Test
    void save_all_insertsAll() {
        List<AppUser> saved = userRepo.saveAll(List.of(
                new AppUser(null, "a", 1), new AppUser(null, "b", 2)));
        assertThat(saved).hasSize(2);
        assertThat(userRepo.count()).isEqualTo(2L);
    }

    @Test
    void save_noIdEntity_insertsExcludingId() {
        NoIdEntity e = new NoIdEntity("noid");
        noIdRepo.save(e);
        assertThat(noIdRepo.count()).isEqualTo(1L);
    }

    @Test
    void save_onlyIdEntity_nullId_throwsNoInsertableFields() {
        OnlyIdEntity e = new OnlyIdEntity(null);
        assertThatThrownBy(() -> onlyIdRepo.save(e))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("无可插入字段");
    }

    // ===================== save 生成策略 =====================

    @Test
    void save_uuidGenerated() {
        UuidEntity e = new UuidEntity(null, "x");
        uuidRepo.save(e);
        assertThat(e.getId()).isNotBlank();
        assertThat(uuidRepo.findById(e.getId())).isPresent();
    }

    @Test
    void save_sequenceGenerated() throws InterruptedException {
        SeqEntity e = new SeqEntity(null, "x");
        seqRepo.save(e);
        assertThat(e.getId()).isNotNull();
        // 雪花生成器每次 save 新建实例，同毫秒会产出相同 ID，稍作等待保证时间戳不同
        Thread.sleep(2);
        SeqEntity e2 = new SeqEntity(null, "y");
        seqRepo.save(e2);
        assertThat(e2.getId()).isNotEqualTo(e.getId());
    }

    @Test
    void save_assignedId_insertThenUpsert() {
        AssignedEntity e = new AssignedEntity(10L, "first");
        assignedRepo.save(e);
        assertThat(assignedRepo.findById(10L)).isPresent();

        // 同主键再次 save -> insert 冲突 -> 回退 update
        e.setName("updated");
        assignedRepo.save(e);
        assertThat(assignedRepo.findById(10L).orElseThrow().getName()).isEqualTo("updated");
    }

    // ===================== 分页 =====================

    @Test
    void page_returnsSlice() {
        for (int i = 0; i < 15; i++) {
            userRepo.save(new AppUser(null, "user" + i, i % 3));
        }
        Pageable pageable = PageRequest.of(1, 10).toPageable();
        Page<AppUser> page = userRepo.page(pageable);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotal()).isEqualTo(15L);
        assertThat(page.getPage()).isZero();
    }

    @Test
    void page_withSort_appendsOrderBy() {
        userRepo.save(new AppUser(null, "b", 1));
        userRepo.save(new AppUser(null, "a", 1));
        userRepo.save(new AppUser(null, "c", 1));
        Pageable pageable = PageRequest.of(1, 10,
                cn.jowen.framework.data.core.sort.Sort.by("name")).toPageable();
        Page<AppUser> page = userRepo.page(pageable);
        assertThat(page.getTotal()).isEqualTo(3L);
        assertThat(page.getContent()).extracting(AppUser::getName)
                .containsExactly("a", "b", "c");
    }

    @Test
    void findAll_pageable_returnsPage() {
        for (int i = 0; i < 15; i++) {
            userRepo.save(new AppUser(null, "user" + i, i % 3));
        }
        Pageable pageable = PageRequest.of(1, 10).toPageable();
        Page<AppUser> page = userRepo.findAll(pageable);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotal()).isEqualTo(15L);
    }

    @Test
    void page_withMultiColumnSort_appendsOrderBy() {
        userRepo.save(new AppUser(null, "a", 2));
        userRepo.save(new AppUser(null, "a", 1));
        userRepo.save(new AppUser(null, "b", 1));
        Pageable pageable = PageRequest.of(1, 10,
                cn.jowen.framework.data.core.sort.Sort.by("name")
                        .and(cn.jowen.framework.data.core.sort.Sort.by("status",
                                cn.jowen.framework.data.core.sort.Direction.DESC)))
                .toPageable();
        Page<AppUser> page = userRepo.page(pageable);
        assertThat(page.getTotal()).isEqualTo(3L);
        assertThat(page.getContent()).extracting(AppUser::getStatus)
                .containsExactly(2, 1, 1);
    }

    @Test
    void page_withWrapper_filters() {
        userRepo.save(new AppUser(null, "a", 0));
        userRepo.save(new AppUser(null, "b", 1));
        userRepo.save(new AppUser(null, "c", 1));
        QueryWrapper<AppUser> wrapper = new QueryWrapper<AppUser>().eq(col("status"), 1);
        Page<AppUser> page = userRepo.page(PageRequest.of(1, 10).toPageable(), wrapper);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotal()).isEqualTo(2L);
    }

    @Test
    void page_withWrapper_inCondition() {
        userRepo.save(new AppUser(null, "a", 0));
        userRepo.save(new AppUser(null, "b", 1));
        userRepo.save(new AppUser(null, "c", 2));
        QueryWrapper<AppUser> wrapper = new QueryWrapper<AppUser>().in(col("status"), List.of(0, 2));
        Page<AppUser> page = userRepo.page(PageRequest.of(1, 10).toPageable(), wrapper);
        assertThat(page.getContent()).hasSize(2);
    }

    // ===================== 原生 SQL =====================

    @Test
    void query_returnsMaps() {
        userRepo.save(new AppUser(null, "tom", 1));
        List<Map<String, Object>> rows = userRepo.query("SELECT name FROM app_user WHERE status = ?", 1);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("name", "tom");
    }

    @Test
    void queryOne_hitAndMiss() {
        userRepo.save(new AppUser(null, "tom", 1));
        assertThat(userRepo.queryOne("SELECT name FROM app_user WHERE name = ?", "tom")).isNotNull();
        assertThat(userRepo.queryOne("SELECT name FROM app_user WHERE name = ?", "nope")).isNull();
    }

    @Test
    void update_sql_updates() {
        AppUser u = new AppUser(null, "tom", 1);
        userRepo.save(u);
        int n = userRepo.update("UPDATE app_user SET status = 9 WHERE id = ?", u.getId());
        assertThat(n).isEqualTo(1);
        assertThat(userRepo.findById(u.getId()).orElseThrow().getStatus()).isEqualTo(9);
    }

    @Test
    void getIdGenerator_returnsConfigured() {
        assertThat(userRepo.getIdGenerator()).isInstanceOf(DefaultIdGenerator.class);
    }

    private static <T> QueryWrapper.ResolvableFunction<T, Object> col(String name) {
        return new QueryWrapper.ResolvableFunction<>() {
            @Override
            public Object apply(T t) {
                return null;
            }

            @Override
            public String getColumnName() {
                return name;
            }
        };
    }

    // ===================== 测试实体 =====================

    @Table("app_user")
    static class AppUser {
        @Id
        @GeneratedValue(GeneratedValue.Strategy.AUTO)
        private Long id;
        private String name;
        private Integer status;

        AppUser(Long id, String name, Integer status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getStatus() {
            return status;
        }

        public void setStatus(Integer status) {
            this.status = status;
        }
    }

    @Table("app_no_id")
    static class NoIdEntity {
        private String name;

        NoIdEntity(String name) {
            this.name = name;
        }
    }

    @Table("app_only_id")
    static class OnlyIdEntity {
        @Id
        private Long id;

        OnlyIdEntity(Long id) {
            this.id = id;
        }
    }

    @Table("app_assigned")
    static class AssignedEntity {
        @Id
        private Long id;
        private String name;

        AssignedEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Table("app_uuid")
    static class UuidEntity {
        @Id
        @cn.jowen.framework.data.core.meta.GeneratedValue(cn.jowen.framework.data.core.meta.GeneratedValue.Strategy.UUID)
        private String id;
        private String name;

        UuidEntity(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }
    }

    @Table("app_seq")
    static class SeqEntity {
        @Id
        @GeneratedValue(GeneratedValue.Strategy.SNOWFLAKE)
        private Long id;
        private String name;

        SeqEntity(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public Long getId() {
            return id;
        }
    }
}
