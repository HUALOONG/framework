package cn.jowen.framework.data.core.datapermission;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据权限规则：根据数据范围生成对应的过滤条件片段。
 *
 * <p>框架默认实现仅对 {@link DataScope#ALL} 返回 {@code null}（不过滤），
 * 其余范围需业务方注入实现（结合当前用户 ID / 部门 ID 生成条件）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface DataPermissionRule {

    /**
     * 生成过滤条件。
     *
     * @param scope 数据范围
     * @param table 作用表名，为空表示作用于全部表
     * @return 条件片段（SQL 片段或持久层可解释的表达式）；无需过滤返回 {@code null}
     */
    @Nullable String condition(DataScope scope, String table);

    /**
     * 默认实现：仅 {@link DataScope#ALL} 不过滤，其余范围返回空条件（由业务实现覆盖）。
     */
    @NullMarked
    final class Default implements DataPermissionRule {
        /**
         * 执行condition操作。
         * @param scope 参数 scope
         * @param table 参数 table
         * @return 结果
         */
        @Override
        public @Nullable String condition(DataScope scope, String table) {
            return scope == DataScope.ALL ? null : "";
        }
    }
}
