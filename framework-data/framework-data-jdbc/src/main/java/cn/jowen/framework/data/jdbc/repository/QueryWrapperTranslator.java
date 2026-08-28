package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.query.Condition;
import cn.jowen.framework.data.core.query.Operator;
import cn.jowen.framework.data.core.query.QueryWrapper;
import cn.jowen.framework.data.jdbc.statement.SqlResult;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * 查询条件翻译器：将 {@link QueryWrapper} 翻译为可执行 {@link SqlResult}（含 {@code SELECT} 与有序参数）。
 *
 * <p>支持 EQ/NE/LIKE/LIKE_LEFT/LIKE_RIGHT/GT/GTE/LT/LTE/IN/NOT_IN/BETWEEN/IS_NULL/IS_NOT_NULL，
 * 以及 AND/OR 逻辑连接；GROUP BY 与 ORDER BY 分别由 {@link QueryWrapper#getGroupBy()}
 * 与 {@link QueryWrapper#getOrderBy()} 直接拼接。
 * 分页（LIMIT/OFFSET）交由方言在仓库层统一处理，本翻译器不产出分页片段。
 *
 * <p>join 暂未实现（TODO）：当前直接忽略，避免破坏基础单表查询。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class QueryWrapperTranslator {

    /**
     * 翻译查询条件为 SQL。
     *
     * @param wrapper   查询条件，不可为 {@code null}
     * @param tableName  表名，不可为 {@code null}
     * @param <T>        实体类型
     * @return 可执行 SQL 结果
     */
    public <T> SqlResult translate(QueryWrapper<T> wrapper, String tableName) {
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();
        boolean needConnector = false;
        for (Condition condition : wrapper.getConditions()) {
            Operator op = condition.getOperator();
            if (op == Operator.AND) {
                needConnector = true;
                continue;
            }
            if (op == Operator.OR) {
                if (where.length() > 0) {
                    where.append(" OR ");
                }
                needConnector = false;
                continue;
            }
            if (op == Operator.NOT) {
                if (where.length() > 0) {
                    where.append(" AND ");
                }
                where.append(" NOT ");
                needConnector = false;
                continue;
            }
            if (needConnector && where.length() > 0) {
                where.append(" AND ");
            }
            needConnector = true;
            appendCondition(where, params, op, condition.getColumn(), condition.getValue());
        }

        String base = "SELECT * FROM " + tableName;
        if (where.length() > 0) {
            base += " WHERE " + where;
        }
        List<String> groupBy = wrapper.getGroupBy();
        if (!groupBy.isEmpty()) {
            base += " GROUP BY " + String.join(", ", groupBy);
        }
        List<String> orderBy = wrapper.getOrderBy();
        if (!orderBy.isEmpty()) {
            base += " ORDER BY " + String.join(", ", orderBy);
        }
        return new SqlResult(base, params);
    }

    private void appendCondition(StringBuilder sb, List<Object> params, Operator op, String column,
                                 @Nullable Object value) {
        switch (op) {
            case EQ -> {
                sb.append(column).append(" = ?");
                params.add(value);
            }
            case NE -> {
                sb.append(column).append(" <> ?");
                params.add(value);
            }
            case LIKE -> {
                sb.append(column).append(" LIKE ?");
                params.add("%" + value + "%");
            }
            case LIKE_LEFT -> {
                sb.append(column).append(" LIKE ?");
                params.add("%" + value);
            }
            case LIKE_RIGHT -> {
                sb.append(column).append(" LIKE ?");
                params.add(value + "%");
            }
            case GT -> {
                sb.append(column).append(" > ?");
                params.add(value);
            }
            case GTE -> {
                sb.append(column).append(" >= ?");
                params.add(value);
            }
            case LT -> {
                sb.append(column).append(" < ?");
                params.add(value);
            }
            case LTE -> {
                sb.append(column).append(" <= ?");
                params.add(value);
            }
            case IN -> {
                sb.append(column).append(" IN ").append(inClause(params, value));
            }
            case NOT_IN -> {
                sb.append(column).append(" NOT IN ").append(inClause(params, value));
            }
            case BETWEEN -> {
                Object[] range = (Object[]) value;
                sb.append(column).append(" BETWEEN ? AND ?");
                params.add(range[0]);
                params.add(range[1]);
            }
            case IS_NULL -> sb.append(column).append(" IS NULL");
            case IS_NOT_NULL -> sb.append(column).append(" IS NOT NULL");
            default -> throw new UnsupportedOperationException("不支持的查询操作符：" + op);
        }
    }

    private String inClause(List<Object> params, @Nullable Object value) {
        Collection<?> items = value instanceof Collection<?> collection
                ? collection
                : (value != null && value.getClass().isArray() ? Arrays.asList((Object[]) value) : List.of());
        StringBuilder sb = new StringBuilder("(");
        boolean first = true;
        for (Object item : items) {
            if (!first) {
                sb.append(", ");
            }
            sb.append("?");
            params.add(item);
            first = false;
        }
        if (first) {
            sb.append("?");
        }
        sb.append(")");
        return sb.toString();
    }
}
