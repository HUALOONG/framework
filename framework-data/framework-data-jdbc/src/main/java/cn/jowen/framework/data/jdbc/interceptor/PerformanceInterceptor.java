package cn.jowen.framework.data.jdbc.interceptor;

import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能拦截器：统计每条 SQL 耗时，超过 {@link JdbcProperties#getSlowSqlThreshold()}（默认 500ms）打印 WARN 日志。
 *
 * <p>维护一个轻量慢 SQL 计数（{@link #getSlowSqlCount()}），不依赖任何三方监控组件（如 Micrometer），
 * 保持零外部依赖。如需对接 Micrometer，可通过反射探测后上报（此处未实现，注释说明）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class PerformanceInterceptor implements SqlInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceInterceptor.class);

    private final JdbcProperties properties;
    private final AtomicLong slowSqlCount = new AtomicLong(0L);

    public PerformanceInterceptor(JdbcProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(SqlContext ctx, InterceptorChain chain) {
        Object result = ctx.proceed();
        long elapsed = ctx.getElapsedMillis();
        if (elapsed > properties.getSlowSqlThreshold()) {
            slowSqlCount.incrementAndGet();
            LOGGER.warn("Slow SQL detected ({}ms > {}ms): {}", elapsed, properties.getSlowSqlThreshold(), ctx.getSql());
        }
        return result;
    }

    /**
     * 累计慢 SQL 次数。
     *
     * @return 慢 SQL 计数
     */
    public long getSlowSqlCount() {
        return slowSqlCount.get();
    }
}
