package cn.jowen.framework.data.jdbc.interceptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * SQL 拦截上下文：在一次 SQL 执行链中传递 SQL、参数、计时与连接，并提供 {@link #proceed()} 驱动责任链。
 *
 * <p>拦截器可读取并改写 {@link #getSql()}/{@link #getParams()}（如 {@code TenantInterceptor} 追加租户条件），
 * 最终由 JdbcTemplate 提供的终止执行器（terminal）落地到数据库。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SqlContext {

    /** SQL 执行类型。 */
    public enum Kind {
        QUERY, UPDATE, INSERT, EXECUTE, BATCH
    }

    private String sql;
    private final List<Object> params;
    private final long startTime = System.nanoTime();
    private @Nullable Connection connection;
    private Kind kind = Kind.QUERY;

    private InterceptorChain chain;
    private Supplier<Object> terminal;
    private final AtomicInteger cursor = new AtomicInteger(0);

    public SqlContext(String sql, List<Object> params) {
        this.sql = sql;
        this.params = new ArrayList<>(params);
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getParams() {
        return params;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getElapsedMillis() {
        return (System.nanoTime() - startTime) / 1_000_000;
    }

    public @Nullable Connection getConnection() {
        return connection;
    }

    public void setConnection(@Nullable Connection connection) {
        this.connection = connection;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    void bind(InterceptorChain chain, Supplier<Object> terminal) {
        this.chain = chain;
        this.terminal = terminal;
        this.cursor.set(0);
    }

    /**
     * 继续责任链；若已无拦截器则执行终止执行器（真正访问数据库）。
     *
     * @return 执行结果
     */
    public Object proceed() {
        List<SqlInterceptor> list = chain.interceptors();
        if (cursor.get() < list.size()) {
            SqlInterceptor interceptor = list.get(cursor.getAndIncrement());
            return interceptor.intercept(this, chain);
        }
        return terminal.get();
    }

    @Override
    public String toString() {
        return "SqlContext{kind=" + kind + ", sql=" + sql + ", params=" + params + "}";
    }
}
