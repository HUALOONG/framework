package cn.jowen.framework.data.mybatis.query;

import cn.jowen.framework.data.core.query.Condition;
import cn.jowen.framework.data.core.query.Operator;
import cn.jowen.framework.data.core.query.QueryWrapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * data-core {@link QueryWrapper} 与 MyBatis Flex {@code QueryWrapper} 之间的条件操作符映射表。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
final class ConditionMapper {

    private ConditionMapper() { }

    /**
     * 将框架操作符翻译为 Flex SQL 条件字符串。
     *
     * @param condition 条件
     * @return 条件 SQL 片段
     */
    static String toSql(Condition condition) {
        String column = condition.getColumn();
        Object value = condition.getValue();
        return switch (condition.getOperator()) {
            case EQ, NE, LIKE, LIKE_LEFT, LIKE_RIGHT, GT, GTE, LT, LTE,
                 IN, NOT_IN, BETWEEN, IS_NULL, IS_NOT_NULL -> build(condition);
            case AND, OR, NOT -> condition.getOperator().name() + "(" + column + ")";
        };
    }

    /**
     * 将多个条件组装为 WHERE 子句。
     *
     * @param conditions 条件列表
     * @return WHERE 子句字符串
     */
    static String toWhereClause(List<Condition> conditions) {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        List<String> clauses = conditions.stream()
                .map(ConditionMapper::toSql)
                .toList();
        if (clauses.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("WHERE ");
        for (int i = 0; i < clauses.size(); i++) {
            if (i > 0) {
                sb.append(" AND ");
            }
            sb.append(clauses.get(i));
        }
        return sb.toString();
    }

    private static String build(Condition c) {
        String col = c.getColumn();
        Operator op = c.getOperator();
        return switch (op) {
            case NE -> col + " <> " + formatValue(c.getValue());
            case LIKE -> col + " LIKE '%" + escapeLike(formatValue(c.getValue())) + "%'";
            case LIKE_LEFT -> col + " LIKE '%" + escapeLike(formatValue(c.getValue())) + "'";
            case LIKE_RIGHT -> col + " LIKE '" + escapeLike(formatValue(c.getValue())) + "%'";
            case GT -> col + " > " + formatValue(c.getValue());
            case GTE -> col + " >= " + formatValue(c.getValue());
            case LT -> col + " < " + formatValue(c.getValue());
            case LTE -> col + " <= " + formatValue(c.getValue());
            case IS_NULL -> col + " IS NULL";
            case IS_NOT_NULL -> col + " IS NOT NULL";
            case IN -> {
                Object v = c.getValue();
                if (v instanceof Object[] arr) {
                    yield col + " IN (" + toCsv(arr) + ")";
                } else if (v instanceof List<?> list) {
                    yield col + " IN (" + toCsv(list.toArray()) + ")";
                } else {
                    yield col + " IN (" + formatValue(v) + ")";
                }
            }
            case NOT_IN -> {
                Object v = c.getValue();
                if (v instanceof Object[] arr) {
                    yield col + " NOT IN (" + toCsv(arr) + ")";
                } else if (v instanceof List<?> list) {
                    yield col + " NOT IN (" + toCsv(list.toArray()) + ")";
                } else {
                    yield col + " NOT IN (" + formatValue(v) + ")";
                }
            }
            case BETWEEN -> {
                Object v = c.getValue();
                if (v instanceof Object[] arr && arr.length >= 2) {
                    yield col + " BETWEEN " + formatValue(arr[0]) + " AND " + formatValue(arr[1]);
                }
                yield col + " BETWEEN ? AND ?";
            }
            default -> col + " = " + formatValue(c.getValue());
        };
    }

    private static String formatValue(@Nullable Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + value + "'";
        }
        return String.valueOf(value);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String toCsv(Object[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatValue(arr[i]));
        }
        return sb.toString();
    }
}
