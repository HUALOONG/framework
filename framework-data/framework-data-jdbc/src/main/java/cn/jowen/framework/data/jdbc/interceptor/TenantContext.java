package cn.jowen.framework.data.jdbc.interceptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 多租户上下文：基于 {@link ThreadLocal} 传递当前租户标识，供 {@link TenantInterceptor} 注入查询条件。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class TenantContext {

    private static final ThreadLocal<@Nullable Object> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(@Nullable Object tenantId) {
        CURRENT.set(tenantId);
    }

    public static @Nullable Object get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
