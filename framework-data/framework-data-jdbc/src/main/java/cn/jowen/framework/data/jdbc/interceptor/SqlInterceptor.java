package cn.jowen.framework.data.jdbc.interceptor;

import org.jspecify.annotations.NullMarked;

/**
 * SQL 拦截器：在 SQL 真正执行前后插入横切逻辑（日志、性能、多租户等）。
 *
 * <p>实现可读取/改写 {@link SqlContext} 中的 SQL 与参数，并通过 {@code ctx.proceed()} 继续责任链。
 * 不调用 {@code proceed()} 将中断执行（如权限拦截器可短路返回）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface SqlInterceptor {

    /**
     * 拦截处理。
     *
     * @param ctx   拦截上下文，不可为 {@code null}
     * @param chain 责任链，不可为 {@code null}
     * @return 执行结果
     */
    Object intercept(SqlContext ctx, InterceptorChain chain);
}
