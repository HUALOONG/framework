package cn.jowen.framework.data.jdbc.interceptor;

import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * 多租户拦截器：开启 {@link JdbcProperties#isTenantEnabled()} 且当前线程设置了租户标识时，
 * 对含 {@code WHERE} 的 SQL 追加 {@code AND <tenantColumn> = ?} 条件。
 *
 * <p>仅改写带 {@code WHERE} 的语句（INSERT 等无 WHERE 语句不追加，避免产生非法 SQL）。
 * 租户值取自 {@link TenantContext}。本拦截器以无副作用方式改写 {@link SqlContext} 的 SQL 与参数。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class TenantInterceptor implements SqlInterceptor {

    private final JdbcProperties properties;

    public TenantInterceptor(JdbcProperties properties) {
        this.properties = properties;
    }

    @Override
    public Object intercept(SqlContext ctx, InterceptorChain chain) {
        if (properties.isTenantEnabled()) {
            Object tenant = TenantContext.get();
            if (tenant != null && containsWhere(ctx.getSql())) {
                String column = properties.getTenantColumn();
                ctx.setSql(ctx.getSql() + " AND " + column + " = ?");
                List<Object> params = new ArrayList<>(ctx.getParams());
                params.add(tenant);
                ctx.getParams().clear();
                ctx.getParams().addAll(params);
            }
        }
        return ctx.proceed();
    }

    private boolean containsWhere(String sql) {
        return sql.toUpperCase().contains(" WHERE ");
    }
}
