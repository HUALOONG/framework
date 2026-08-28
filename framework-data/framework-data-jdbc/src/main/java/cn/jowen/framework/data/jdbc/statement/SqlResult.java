package cn.jowen.framework.data.jdbc.statement;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * SQL 构建结果：承载最终 SQL 文本与有序参数列表。
 *
 * <p>仓库层 / 模板层统一以 {@link SqlResult} 在组件间传递「可执行的 SQL + 参数」，避免散落的字符串拼接。
 *
 * @param sql    SQL 文本（含 {@code ?} 占位符），不可为 {@code null}
 * @param params 有序参数列表，不可为 {@code null}
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record SqlResult(String sql, List<Object> params) {

    /**
     * 将参数列表转为数组（便于 {@code PreparedStatement} 绑定）。
     *
     * @return 参数数组，不可为 {@code null}
     */
    public Object[] toParamArray() {
        return params.toArray();
    }

    /**
     * 返回参数个数。
     *
     * @return 参数个数
     */
    public int paramCount() {
        return params.size();
    }

    @Override
    public @Nullable String toString() {
        return sql + " " + params;
    }
}
