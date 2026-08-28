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
 * @since 0.0.1
 * @version 0.0.1
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
            if (tenant != null && hasTopLevelWhere(ctx.getSql())) {
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

    /**
     * 判断 SQL 是否含有处于最外层的 {@code WHERE} 关键字。
     *
     * <p>通过括号深度计数扫描，仅在深度为 0 时识别到的 {@code WHERE} 才视为顶层条件，
     * 避免误匹配子查询（如 {@code SELECT * FROM a WHERE x IN (SELECT ... WHERE ...)}）内部的 WHERE，
     * 也避免对不存在顶层 WHERE 的语句错误追加条件。字符串字面量与注释内的内容不参与计数。
     */
    private boolean hasTopLevelWhere(String sql) {
        if (sql == null || sql.isEmpty()) {
            return false;
        }
        String upper = sql.toUpperCase();
        int depth = 0;
        int i = 0;
        int n = upper.length();
        while (i < n) {
            char c = upper.charAt(i);
            if (c == '\'') {
                // 跳过字符串字面量
                i++;
                while (i < n && upper.charAt(i) != '\'') {
                    i++;
                }
                i++;
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth > 0) depth--;
            }
            if (depth == 0 && i + 5 < n
                    && upper.startsWith("WHERE", i)
                    && !Character.isLetterOrDigit(upper.charAt(i + 5))
                    && (i == 0 || !Character.isLetterOrDigit(upper.charAt(i - 1)))) {
                // 命中顶层 WHERE 关键字（前后非字母数字，避免误认 WHEREVER 之类）
                return true;
            }
            i++;
        }
        return false;
    }
}
