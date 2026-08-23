package cn.jowen.framework.extras.datapermission;

import org.jspecify.annotations.NullMarked;

/**
 * 数据权限拦截器（骨架）。
 *
 * <p>拦截 SQL → 解析 {@link DataPermission} 注解 → 取用户权限范围 → 改写 SQL 追加条件。
 * 当前由 {@link DataPermissionAspect} 承担 AOP 切面职责，本类保留作为 MyBatis 拦截器模式扩展点。
 *
 * @author 王飞
 * @since 2026-08-25
 * @see DataPermissionAspect
 */
@NullMarked
public final class DataPermissionInterceptor {

    // TODO: 实现数据权限拦截器逻辑（当前由 DataPermissionAspect 实现）
}
