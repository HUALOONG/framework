package cn.jowen.framework.data.mybatis.query;

import cn.jowen.framework.data.core.query.Condition;
import cn.jowen.framework.data.core.query.QueryWrapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * data-core {@link QueryWrapper} 到 SQL 字符串的翻译器。
 * <p>
 * 纯函数、无状态。将条件列表翻译为 WHERE 子句、排序翻译为 ORDER BY 子句、
 * limit/offset 翻译为 LIMIT 子句。
 * </p>
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
public final class FlexQueryWrapperTranslator {

    private FlexQueryWrapperTranslator() { }

    /**
     * 翻译条件为 WHERE 子句。
     *
     * @param conditions 条件列表
     * @return WHERE 子句
     */
    public static String toWhereClause(List<Condition> conditions) {
        return ConditionMapper.toWhereClause(conditions);
    }

    /**
     * 翻译排序为 ORDER BY 子句。
     *
     * @param orderBy 排序字符串列表
     * @return ORDER BY 子句
     */
    public static String toOrderByClause(List<String> orderBy) {
        if (orderBy == null || orderBy.isEmpty()) {
            return "";
        }
        return "ORDER BY " + String.join(", ", orderBy);
    }

    /**
     * 翻译 limit/offset 为 LIMIT 子句。
     *
     * @param limit  最大行数
     * @param offset 偏移量
     * @return LIMIT 子句
     */
    public static String toLimitClause(@Nullable Integer limit, @Nullable Integer offset) {
        StringBuilder sb = new StringBuilder();
        if (offset != null) {
            sb.append("OFFSET ").append(offset);
        }
        if (limit != null) {
            sb.append(limit > 0 ? " LIMIT " + limit : " LIMIT -1");
        }
        return sb.toString();
    }

    /**
     * 翻译整个 QueryWrapper 为 SQL 片段（不含 SELECT 和 FROM）。
     *
     * @param wrapper 查询包装器
     * @return WHERE + ORDER BY + LIMIT 子句
     */
    public static String toSqlFragments(QueryWrapper<?> wrapper) {
        StringBuilder sb = new StringBuilder();
        String where = ConditionMapper.toWhereClause(wrapper.getConditions());
        if (!where.isEmpty()) {
            sb.append(where).append(" ");
        }
        String orderBy = toOrderByClause(wrapper.getOrderBy());
        if (!orderBy.isEmpty()) {
            sb.append(orderBy).append(" ");
        }
        String limit = toLimitClause(wrapper.getLimit(), wrapper.getOffset());
        if (!limit.isEmpty()) {
            sb.append(limit).append(" ");
        }
        return sb.toString().trim();
    }
}
