package cn.jowen.framework.data.jdbc.interceptor;

import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能拦截器：统计每条 SQL 耗时，超过 {@link JdbcProperties#getSlowSqlThreshold()}（默认 500ms）打印 WARN 日志。
 *
 * <p>维护一个轻量慢 SQL 计数（{@link #getSlowSqlCount()}）。可选传入 {@link MeterRegistry} 上报
 * {@code framework.jdbc.sql} 耗时 Timer（Micrometer 2.0）；不传则保持零外部依赖。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class PerformanceInterceptor implements SqlInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PerformanceInterceptor.class);

    private static final String SQL_TIMER_NAME = "framework.jdbc.sql";

    private final JdbcProperties properties;
    private final @Nullable MeterRegistry meterRegistry;
    private final AtomicLong slowSqlCount = new AtomicLong(0L);

    public PerformanceInterceptor(JdbcProperties properties) {
        this(properties, null);
    }

    public PerformanceInterceptor(JdbcProperties properties, @Nullable MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Object intercept(SqlContext ctx, InterceptorChain chain) {
        Object result = ctx.proceed();
        long elapsed = ctx.getElapsedMillis();
        if (meterRegistry != null) {
            Timer.builder(SQL_TIMER_NAME)
                    .description("SQL execution time")
                    .register(meterRegistry)
                    .record(elapsed, TimeUnit.MILLISECONDS);
        }
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
