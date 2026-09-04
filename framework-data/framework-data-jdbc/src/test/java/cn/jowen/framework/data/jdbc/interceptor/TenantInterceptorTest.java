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
}
