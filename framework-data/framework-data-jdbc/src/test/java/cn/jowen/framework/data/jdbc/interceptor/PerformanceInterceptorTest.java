package cn.jowen.framework.data.jdbc.interceptor;

import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PerformanceInterceptor}、{@link InterceptorChain} 与 {@link SqlContext} 测试。
 */
class PerformanceInterceptorTest {

    @Test
    void fastSql_doesNotIncrementSlowCount() {
        JdbcProperties props = new JdbcProperties();
        PerformanceInterceptor interceptor = new PerformanceInterceptor(props);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(interceptor);

        Object result = chain.execute(() -> "done", new SqlContext("SELECT 1", List.of()));

        assertThat(result).isEqualTo("done");
        assertThat(interceptor.getSlowSqlCount()).isZero();
    }

    @Test
    void slowSql_incrementsSlowCount() {
        JdbcProperties props = new JdbcProperties();
        // elapsed 恒 >= 0，阈值 -1 保证任何执行都触发慢 SQL 分支
        props.setSlowSqlThreshold(-1);
        PerformanceInterceptor interceptor = new PerformanceInterceptor(props);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(interceptor);

        Object result = chain.execute(() -> "done", new SqlContext("SELECT 1", List.of()));

        assertThat(result).isEqualTo("done");
        assertThat(interceptor.getSlowSqlCount()).isEqualTo(1);
    }

    @Test
    void meterRegistry_recordsTimer() {
        io.micrometer.core.instrument.MeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        JdbcProperties props = new JdbcProperties();
        PerformanceInterceptor interceptor = new PerformanceInterceptor(props, registry);
        InterceptorChain chain = new InterceptorChain();
        chain.addInterceptor(interceptor);

        chain.execute(() -> "done", new SqlContext("SELECT 1", List.of()));

        io.micrometer.core.instrument.Timer timer =
                registry.find("framework.jdbc.sql").timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1L);
    }

    @Test
    void chain_empty_executesTerminalDirectly() {
        InterceptorChain chain = new InterceptorChain();
        SqlContext ctx = new SqlContext("SELECT 1", List.of());

        assertThat(chain.isEmpty()).isTrue();
        assertThat(chain.execute(() -> "terminal", ctx)).isEqualTo("terminal");
    }

    @Test
    void chain_addInterceptorAtIndex_andView() {
        InterceptorChain chain = new InterceptorChain();
        SqlInterceptor first = (c, ch) -> c.proceed();
        SqlInterceptor second = (c, ch) -> c.proceed();
        chain.addInterceptor(first);
        chain.addInterceptor(0, second);

        assertThat(chain.isEmpty()).isFalse();
        assertThat(chain.interceptors()).containsExactly(second, first);
    }

    @Test
    void context_gettersAndMutators() {
        SqlContext ctx = new SqlContext("SELECT 1", List.of("a"));

        assertThat(ctx.getSql()).isEqualTo("SELECT 1");
        assertThat(ctx.getParams()).containsExactly("a");
        assertThat(ctx.getKind()).isEqualTo(SqlContext.Kind.QUERY);
        assertThat(ctx.getStartTime()).isPositive();
        assertThat(ctx.getElapsedMillis()).isGreaterThanOrEqualTo(0);

        ctx.setSql("UPDATE t SET a = 1");
        ctx.setKind(SqlContext.Kind.UPDATE);
        assertThat(ctx.getSql()).isEqualTo("UPDATE t SET a = 1");
        assertThat(ctx.getKind()).isEqualTo(SqlContext.Kind.UPDATE);
        assertThat(ctx.getConnection()).isNull();
        assertThat(ctx.toString()).contains("UPDATE");
    }

    @Test
    void context_kindValues() {
        assertThat(SqlContext.Kind.values()).containsExactly(
                SqlContext.Kind.QUERY, SqlContext.Kind.UPDATE, SqlContext.Kind.INSERT,
                SqlContext.Kind.EXECUTE, SqlContext.Kind.BATCH);
    }
}
