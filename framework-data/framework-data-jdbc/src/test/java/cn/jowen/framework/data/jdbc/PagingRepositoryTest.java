package cn.jowen.framework.data.jdbc;

import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.page.Page;
import cn.jowen.framework.data.core.page.Pageable;
import cn.jowen.framework.data.core.page.PageRequest;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import cn.jowen.framework.data.jdbc.context.JdbcContextBuilder;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.interceptor.TenantContext;
import cn.jowen.framework.data.jdbc.mapping.BeanPropertyRowMapper;
import cn.jowen.framework.data.jdbc.repository.QueryWrapperTranslator;
import cn.jowen.framework.data.jdbc.statement.SqlResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PagingRepositoryTest 分页查询与 QueryWrapper 条件过滤测试。
 * 通过 JdbcTemplate + DialectRegistry 模拟仓储分页行为。
 */
class PagingRepositoryTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50) NOT NULL,
                status INT DEFAULT 1,
                tenant_id VARCHAR(36)
            );
            """;

    private JdbcContext ctx;
    private JdbcTemplate tpl;
    private BeanPropertyRowMapper<User> mapper;
    private DialectRegistry dialectRegistry;

    @BeforeEach
    void setUp() throws Exception {
        ctx = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:page1;DB_CLOSE_DELAY=-1")
                .username("sa").password("")
                .poolType(PoolType.SIMPLE)
                .build();
        tpl = ctx.getJdbcTemplate();
        mapper = BeanPropertyRowMapper.of(User.class);
        dialectRegistry = ctx.getDialectRegistry();
        tpl.execute(DDL);
        // Cleanup tables to prevent data pollution
        try (java.sql.Connection conn = ctx.getConnectionProvider().getConnection();
             java.sql.Statement st = conn.createStatement()) {
            try { st.execute("DROP TABLE IF EXISTS test_execute"); } catch (Exception ignore) {}
            try { st.execute("DELETE FROM app_user"); } catch (Exception ignore) {}
            try { st.execute("DELETE FROM app_account"); } catch (Exception ignore) {}
        }
        // 插入 25 条测试数据
        for (int i = 1; i <= 25; i++) {
            tpl.executeInsert("INSERT INTO app_user (name, status, tenant_id) VALUES (?, ?, ?)",
                    "app_user" + i, (i % 3 == 0) ? 0 : 1, null);
        }
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        ctx.close();
    }

    // -------------------------------------------------------------------------
    // 分页查询
    // -------------------------------------------------------------------------

    @Test
    void page_firstPage_returnsCorrectSlice() {
        Page<User> page = page(tpl, mapper, dialectRegistry, PageRequest.of(1, 10), null);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotal()).isEqualTo(25L);
        assertThat(page.getPage()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.isFirst()).isTrue();
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void page_secondPage_returnsCorrectSlice() {
        Page<User> page = page(tpl, mapper, dialectRegistry, PageRequest.of(2, 10), null);
        assertThat(page.getContent()).hasSize(10);
        assertThat(page.getTotal()).isEqualTo(25L);
        assertThat(page.isFirst()).isFalse();
        assertThat(page.isLast()).isFalse();
    }

    @Test
    void page_lastPage_smallRemainder() {
        Page<User> page = page(tpl, mapper, dialectRegistry, PageRequest.of(3, 10), null);
        assertThat(page.getContent()).hasSize(5);
        assertThat(page.isLast()).isTrue();
    }

    @Test
    void page_emptyResultWhenBeyondRange() {
        Page<User> page = page(tpl, mapper, dialectRegistry, PageRequest.of(10, 10), null);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotal()).isEqualTo(25L);
        assertThat(page.isEmpty()).isTrue();
    }

    // -------------------------------------------------------------------------
    // QueryWrapper 过滤
    // -------------------------------------------------------------------------

    @Test
    void page_withQueryWrapper_eqCondition() {
        cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, Integer> statusCol =
                new cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, Integer>() {
                    @Override public Integer apply(User u) { return u.getStatus(); }
                    @Override public String getColumnName() { return "status"; }
                };
        cn.jowen.framework.data.core.query.QueryWrapper<User> wrapper =
                new cn.jowen.framework.data.core.query.QueryWrapper<User>().eq(statusCol, 0);
        QueryWrapperTranslator translator = new QueryWrapperTranslator();
        SqlResult result = translator.translate(wrapper, "app_user");
        Page<User> page = pageInternal(tpl, mapper, dialectRegistry, result.sql(), result.params(),
                PageRequest.of(1, 10).toPageable());
        assertThat(page.getContent()).allMatch(u -> u.getStatus() == 0);
        assertThat(page.getTotal()).isEqualTo(8L);
    }

    @Test
    void page_withQueryWrapper_noMatch() {
        cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, String> nameCol =
                new cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, String>() {
                    @Override public String apply(User u) { return u.getName(); }
                    @Override public String getColumnName() { return "name"; }
                };
        cn.jowen.framework.data.core.query.QueryWrapper<User> wrapper =
                new cn.jowen.framework.data.core.query.QueryWrapper<User>().eq(nameCol, "nonexist");
        QueryWrapperTranslator translator = new QueryWrapperTranslator();
        SqlResult result = translator.translate(wrapper, "app_user");
        Page<User> page = pageInternal(tpl, mapper, dialectRegistry, result.sql(), result.params(),
                PageRequest.of(1, 10).toPageable());
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotal()).isEqualTo(0L);
    }

    @Test
    void page_withQueryWrapper_likeCondition() {
        cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, String> nameCol =
                new cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, String>() {
                    @Override public String apply(User u) { return u.getName(); }
                    @Override public String getColumnName() { return "name"; }
                };
        cn.jowen.framework.data.core.query.QueryWrapper<User> wrapper =
                new cn.jowen.framework.data.core.query.QueryWrapper<User>().like(nameCol, "user1");
        QueryWrapperTranslator translator = new QueryWrapperTranslator();
        SqlResult result = translator.translate(wrapper, "app_user");
        Page<User> page = pageInternal(tpl, mapper, dialectRegistry, result.sql(), result.params(),
                PageRequest.of(1, 100).toPageable());
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(u -> u.getName().contains("user1"));
    }

    @Test
    void page_withQueryWrapper_gteCondition() {
        cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, Integer> statusCol =
                new cn.jowen.framework.data.core.query.QueryWrapper.ResolvableFunction<User, Integer>() {
                    @Override public Integer apply(User u) { return u.getStatus(); }
                    @Override public String getColumnName() { return "status"; }
                };
        cn.jowen.framework.data.core.query.QueryWrapper<User> wrapper =
                new cn.jowen.framework.data.core.query.QueryWrapper<User>().gte(statusCol, 1);
        QueryWrapperTranslator translator = new QueryWrapperTranslator();
        SqlResult result = translator.translate(wrapper, "app_user");
        Page<User> page = pageInternal(tpl, mapper, dialectRegistry, result.sql(), result.params(),
                PageRequest.of(1, 100).toPageable());
        // status=0 的有 8 条，所以 >=1 的应为 17 条
        assertThat(page.getTotal()).isEqualTo(17L);
    }

    // -------------------------------------------------------------------------
    // count
    // -------------------------------------------------------------------------

    @Test
    void count_returnsTotalRows() {
        Long count = tpl.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        assertThat(count).isEqualTo(25L);
    }

    // -------------------------------------------------------------------------
    // 辅助方法
    // -------------------------------------------------------------------------

    private Page<User> page(JdbcTemplate template, BeanPropertyRowMapper<User> mapper,
                             DialectRegistry registry, PageRequest pr,
                             cn.jowen.framework.data.core.query.QueryWrapper<User> wrapper) {
        String base = "SELECT * FROM app_user";
        List<Object> params = List.of();
        if (wrapper != null) {
            SqlResult result = new QueryWrapperTranslator().translate(wrapper, "app_user");
            base = result.sql();
            params = result.params();
        }
        return pageInternal(template, mapper, registry, base, params, pr.toPageable());
    }

    private Page<User> pageInternal(JdbcTemplate template, BeanPropertyRowMapper<User> mapper,
                                     DialectRegistry registry, String baseSql,
                                     List<Object> params, Pageable pageable) {
        String countSql = "SELECT COUNT(*) FROM (" + stripOrderBy(baseSql) + ") AS _cnt";
        Long total = template.queryForObject(countSql, Long.class, params.toArray());
        String pageSql = registry.getDefault().buildPageSql(baseSql, pageable);
        List<User> content = template.query(pageSql, mapper, params.toArray());
        return new Page<>(content, total == null ? 0L : total, pageable.getPage(), pageable.getSize());
    }

    private static String stripOrderBy(String sql) {
        int idx = sql.toUpperCase().lastIndexOf(" ORDER BY ");
        return idx < 0 ? sql : sql.substring(0, idx);
    }
}
