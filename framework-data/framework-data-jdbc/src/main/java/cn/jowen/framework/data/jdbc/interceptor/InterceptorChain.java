package cn.jowen.framework.data.jdbc.interceptor;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SQL 拦截器责任链：按注册顺序编排 {@link SqlInterceptor}，并最终执行终止执行器（访问数据库）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class InterceptorChain {

    private final List<SqlInterceptor> interceptors = new ArrayList<>();

    /**
     * 注册拦截器（顺序即执行顺序）。
     *
     * @param interceptor 拦截器，不可为 {@code null}
     */
    public void addInterceptor(SqlInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * 注册拦截器（按索引插入）。
     *
     * @param index       插入位置
     * @param interceptor 拦截器，不可为 {@code null}
     */
    public void addInterceptor(int index, SqlInterceptor interceptor) {
        interceptors.add(index, interceptor);
    }

    /**
     * 拦截器列表（供 {@link SqlContext#proceed()} 遍历）。
     *
     * @return 不可变视图，不可为 {@code null}
     */
    public List<SqlInterceptor> interceptors() {
        return List.copyOf(interceptors);
    }

    public boolean isEmpty() {
        return interceptors.isEmpty();
    }

    /**
     * 以终止执行器执行整条责任链。
     *
     * @param terminal 终止执行器（真正访问数据库），不可为 {@code null}
     * @param ctx      上下文，不可为 {@code null}
     * @return 执行结果
     */
    public Object execute(Supplier<Object> terminal, SqlContext ctx) {
        ctx.bind(this, terminal);
        return ctx.proceed();
    }
}
