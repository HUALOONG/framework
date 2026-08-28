package cn.jowen.framework.extras.web.datapermission;

import cn.jowen.framework.extras.common.exception.DataPermissionException;
import cn.jowen.framework.extras.properties.DataScope;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 数据权限规则：根据数据范围生成对应的过滤条件片段。
 *
 * <p>框架默认实现仅对 {@link DataScope#ALL} 返回空条件（不过滤）；
 * 其他范围需由业务方注入实现（结合当前用户所属部门 / 用户 ID 生成条件）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface DataPermissionRule {

    /**
     * 生成过滤条件（SQL 片段或条件表达式，由持久层适配器解释）。
     *
     * @param scope 数据范围
     * @param table 作用表名，为空表示全部表
     * @return 条件片段；无需过滤返回 {@code null}
     */
    @Nullable String condition(DataScope scope, String table);

    /**
     * 兜底实现：仅对 ALL 返回空（不过滤），其余返回空串（不过滤）。
     *
     * <p><b>注意</b>：本实现不产生任何过滤条件，受限范围与 ALL 效果相同，
     * 仅适用于"暂不启用行级过滤、由业务方自行在 SQL 中处理"的场景。
     * 若需要真实的行级过滤，请使用 {@link SqlDefault}。
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

    /**
     * 基于列名的 SQL 条件默认实现：按数据范围生成可直接拼接的行级过滤条件。
     *
     * <p>生成规则（列名为配置项，值取自 {@link DataPermissionUserProvider}）：
     * <ul>
     *   <li>{@link DataScope#ALL}：{@code null}，不过滤；</li>
     *   <li>{@link DataScope#SELF}：{@code user_column = '当前用户ID'}；</li>
     *   <li>{@link DataScope#DEPT}：{@code dept_column = '当前部门ID'}；</li>
     *   <li>{@link DataScope#DEPT_AND_CHILD}：{@code dept_column IN ('部门1','部门2'...)}}；</li>
     *   <li>{@link DataScope#CUSTOM}：{@code null}，交由业务方自行实现。</li>
     * </ul>
     *
     * <p><b>安全约定</b>：受限范围下若无法解析出当前用户（{@code provider} 为
     * {@link DataPermissionUserProvider#NONE} 或返回空值），本实现
     * <b>抛出 {@link DataPermissionException}</b> 而非返回不过滤条件——
     * "取不到用户就放行全部数据"会造成越权且极难排查，故选择显式失败。
     *
     * <p><b>注入防护</b>：值统一经 {@link #escape} 转义单引号。
     * 列名来自应用配置（可信输入），不在此做校验。
     */
    @NullMarked
    final class SqlDefault implements DataPermissionRule {

        /** deptColumn 不可变字段。 */
        private final String deptColumn;
        /** userColumn 不可变字段。 */
        private final String userColumn;
        /** provider 不可变字段。 */
        private final DataPermissionUserProvider provider;

        /**
         * 创建 SQL 条件规则。
         *
         * @param deptColumn 部门列名，默认 {@code dept_id}
         * @param userColumn 用户列名，默认 {@code create_by}
         * @param provider   用户上下文提供者
         */
        public SqlDefault(String deptColumn, String userColumn, DataPermissionUserProvider provider) {
            this.deptColumn = deptColumn;
            this.userColumn = userColumn;
            this.provider = provider;
        }

        /**
         * 执行condition操作。
         * @param scope 参数 scope
         * @param table 参数 table
         * @return 结果
         */
        @Override
        public @Nullable String condition(DataScope scope, String table) {
            return switch (scope) {
                case ALL, CUSTOM -> null;
                case SELF -> userColumn + " = '" + escape(requireUserId(scope)) + "'";
                case DEPT -> deptColumn + " = '" + escape(requireDeptId(scope)) + "'";
                case DEPT_AND_CHILD -> inCondition(requireDeptAndChildIds(scope));
            };
        }

        private String inCondition(List<String> ids) {
            if (ids.isEmpty()) {
                throw new DataPermissionException("数据范围 DEPT_AND_CHILD 需要可见部门列表，但当前为空");
            }
            return ids.stream()
                    .map(id -> "'" + escape(id) + "'")
                    .collect(java.util.stream.Collectors.joining(",", deptColumn + " IN (", ")"));
        }

        private String requireUserId(DataScope scope) {
            String id = provider.currentUserId();
            if (id == null || id.isBlank()) {
                throw new DataPermissionException(
                        "数据范围 " + scope + " 需要当前用户 ID，但未注入 DataPermissionUserProvider 或返回为空");
            }
            return id;
        }

        private String requireDeptId(DataScope scope) {
            String id = provider.currentDeptId();
            if (id == null || id.isBlank()) {
                throw new DataPermissionException(
                        "数据范围 " + scope + " 需要当前部门 ID，但未注入 DataPermissionUserProvider 或返回为空");
            }
            return id;
        }

        private List<String> requireDeptAndChildIds(DataScope scope) {
            List<String> ids = provider.currentDeptAndChildIds();
            if (ids == null || ids.isEmpty()) {
                throw new DataPermissionException(
                        "数据范围 " + scope + " 需要可见部门列表，但未注入 DataPermissionUserProvider 或返回为空");
            }
            return ids;
        }

        /** 转义 SQL 字符串字面量中的单引号，防止拼接待注入。 */
        private static String escape(String value) {
            return value.replace("'", "''");
        }
    }
}
