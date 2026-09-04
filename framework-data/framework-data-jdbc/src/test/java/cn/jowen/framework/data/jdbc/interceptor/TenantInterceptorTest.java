package cn.jowen.framework.data.jdbc.interceptor;

import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TenantInterceptor} 测试：基于真实 {@link SqlContext}/{@link InterceptorChain} 验证
 * 多租户条件追加与各类跳过分支。
 */
class TenantInterceptorTest {

    @AfterEach
    void clear() {
        TenantContext.clear();
    }

    private JdbcProperties properties(boolean enabled, String column) {
        JdbcProperties p = new JdbcProperties();
        p.setTenantEnabled(enabled);
        p.setTenantColumn(column);
        return p;
    }

    private String run(String sql, List<Object> params, boolean enabled, String column, Object tenant) {
        TenantInterceptor interceptor = new TenantInterceptor(properties(enabled, column));
        if (tenant != null) {
            TenantContext.set(tenant);
        }
        SqlContext ctx = new SqlContext(sql, params);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(interceptor);
        chain.execute(() -> "done", ctx);
        return ctx.getSql();
    }

    @Test
    void appendsTenantConditionWhenEnabled() {
        String sql = run("SELECT * FROM t WHERE id = ?", List.of(1), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo("SELECT * FROM t WHERE id = ? AND tenant_id = ?");
    }

    @Test
    void addsTenantParamWhenEnabled() {
        TenantInterceptor interceptor = new TenantInterceptor(properties(true, "tenant_id"));
        TenantContext.set("acme");
        SqlContext ctx = new SqlContext("SELECT * FROM t WHERE id = ?", List.of(1));
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(interceptor);
        chain.execute(() -> "done", ctx);
        assertThat(ctx.getParams()).containsExactly(1, "acme");
    }

    @Test
    void noChangeWhenDisabled() {
        String sql = run("SELECT * FROM t WHERE id = ?", List.of(1), false, "tenant_id", "acme");
        assertThat(sql).isEqualTo("SELECT * FROM t WHERE id = ?");
    }

    @Test
    void noChangeWhenNoTenant() {
        String sql = run("SELECT * FROM t WHERE id = ?", List.of(1), true, "tenant_id", null);
        assertThat(sql).isEqualTo("SELECT * FROM t WHERE id = ?");
    }

    @Test
    void noChangeWhenNoTopLevelWhere() {
        String sql = run("SELECT * FROM t", List.of(), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo("SELECT * FROM t");
    }

    @Test
    void stringLiteralBeforeWhereIsSkipped() {
        // 字符串字面量位于顶层 WHERE 之前：扫描器需进入字面量跳过分支，且不将内部关键字误判为条件
        String sql = run("SELECT 'has WHERE inside' FROM t WHERE x = ?", List.of(1), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo("SELECT 'has WHERE inside' FROM t WHERE x = ? AND tenant_id = ?");
    }

    @Test
    void subqueryBeforeWhereTracksParenDepth() {
        // 括号出现在顶层 WHERE 之前：内部 WHERE 处于深度>0 不应误判；仅真正的顶层 WHERE 生效
        String sql = run("SELECT * FROM (SELECT id FROM s WHERE c = ?) t WHERE a = ?",
                List.of(2, 1), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo(
                "SELECT * FROM (SELECT id FROM s WHERE c = ?) t WHERE a = ? AND tenant_id = ?");
    }

    @Test
    void noChangeForEmptySql() {
        String sql = run("", List.of(), true, "tenant_id", "acme");
        assertThat(sql).isEmpty();
    }

    @Test
    void whereKeywordInsideWord_isNotTopLevel() {
        // "WHEREVER" 不应被识别为顶层 WHERE（前后字母边界检测）
        String sql = run("SELECT * FROM t WHEREVER x = ?", List.of(1), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo("SELECT * FROM t WHEREVER x = ?");
    }

    @Test
    void topLevelWhereAtStart_detected() {
        // WHERE 位于语句起始（i == 0 分支）
        String sql = run("WHERE id = ?", List.of(1), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo("WHERE id = ? AND tenant_id = ?");
    }

    @Test
    void trailingWhere_tokenAtEnd_isNotMatched() {
        // WHERE 处于字符串末尾（i + 5 == n 边界），不应误判为顶层 WHERE
        String sql = run("SELECT * FROM t WHERE", List.of(), true, "tenant_id", "acme");
        assertThat(sql).isEqualTo("SELECT * FROM t WHERE");
    }
}
