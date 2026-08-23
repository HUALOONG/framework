package cn.jowen.framework.data.core.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 单个查询条件，由操作符、列名与值组成。实现层据此拼接 SQL。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class Condition {

    private final Operator operator;
    private final String column;
    private final @Nullable Object value;

    public Condition(Operator operator, String column, @Nullable Object value) {
        this.operator = operator;
        this.column = column;
        this.value = value;
    }

    public Operator getOperator() { return operator; }
    public String getColumn() { return column; }
    public @Nullable Object getValue() { return value; }

    @Override
    public String toString() {
        return operator + "(" + column + ", " + value + ")";
    }
}