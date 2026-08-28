package cn.jowen.framework.data.jdbc.interceptor;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link SqlContext} 单元测试。
 */
class SqlContextTest {

    @Test
    void constructor_setsSqlAndParams() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of(1, 2));
        assertThat(ctx.getSql()).isEqualTo("SELECT 1");
        assertThat(ctx.getParams()).containsExactly(1, 2);
        assertThat(ctx.getConnection()).isNull();
        assertThat(ctx.getKind()).isEqualTo(SqlContext.Kind.QUERY);
    }

    @Test
    void constructor_copiesParamsDefensively() {
        List<Object> params = new java.util.ArrayList<>(List.of(1));
        SqlContext ctx = new SqlContext("SELECT 1", params);
        params.add(2);
        assertThat(ctx.getParams()).containsExactly(1);
    }

    @Test
    void setConnection_updatesConnection() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of());
        Connection connection = mock(Connection.class);

        ctx.setConnection(connection);

        assertThat(ctx.getConnection()).isSameAs(connection);
    }

    @Test
    void setSql_updatesSql() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of());
        ctx.setSql("SELECT 2");
        assertThat(ctx.getSql()).isEqualTo("SELECT 2");
    }

    @Test
    void setKind_updatesKind() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of());
        ctx.setKind(SqlContext.Kind.INSERT);
        assertThat(ctx.getKind()).isEqualTo(SqlContext.Kind.INSERT);
    }

    @Test
    void getElapsedMillis_returnsNonNegative() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of());
        assertThat(ctx.getElapsedMillis()).isGreaterThanOrEqualTo(0L);
        assertThat(ctx.getStartTime()).isGreaterThan(0L);
    }

    @Test
    void proceed_noInterceptors_executesTerminal() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of());
        ctx.bind(new InterceptorChain(), () -> "terminal");

        assertThat(ctx.proceed()).isEqualTo("terminal");
    }

    @Test
    void proceed_withInterceptor_invokesThenTerminal() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of());
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor((c, ch) -> {
            c.setSql(c.getSql() + " /* intercepted */");
            return c.proceed();
        });
        ctx.bind(chain, () -> ctx.getSql());

        assertThat(ctx.proceed()).isEqualTo("SELECT 1 /* intercepted */");
    }

    @Test
    void toString_containsKindSqlAndParams() {
        SqlContext ctx = new SqlContext("SELECT ?", List.of(1));
        ctx.setKind(SqlContext.Kind.UPDATE);

        assertThat(ctx.toString())
                .contains("UPDATE")
                .contains("SELECT ?")
                .contains("1");
    }
}
