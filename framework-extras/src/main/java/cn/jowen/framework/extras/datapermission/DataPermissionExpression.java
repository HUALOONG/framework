package cn.jowen.framework.extras.datapermission;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Objects;

/**
 * 数据权限表达式：SQL 片段 + 参数列表（参数化占位符，禁止字符串内插）。
 *
 * @param sql    SQL WHERE 片段（如 {@code t_user.dept_id IN (?, ?, ?)}），空串表示无限制
 * @param params 占位符对应参数（与 {@code ?} 数量一致）
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record DataPermissionExpression(String sql, List<Object> params) {

    public DataPermissionExpression {
        Objects.requireNonNull(sql, "sql must not be null");
        params = params == null ? List.of() : List.copyOf(params);
    }

    /**
     * 无限制表达式（空 SQL + 空参数）。
     */
    public static DataPermissionExpression empty() {
        return new DataPermissionExpression("", List.of());
    }
}
