package cn.jowen.framework.data.core.query;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 类型安全更新包装器。通过 {@link Function} 方法引用安全指定更新列与筛选条件。
 *
 * <p>使用示例：
 * <pre>{@code
 * UpdateWrapper<Order> w = new UpdateWrapper<>()
 *     .set(Order::getStatus, "SHIPPED")
 *     .eq(Order::getId, orderId);
 * }</pre>
 *
 * @param <T> 实体类型
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class UpdateWrapper<T> {

    private final List<SetClause> setClauses = new ArrayList<>();
    private final List<Condition> conditions = new ArrayList<>();

    public UpdateWrapper() {
    }

    public UpdateWrapper<T> set(Function<T, ?> column, @Nullable Object value) {
        setClauses.add(new SetClause(resolveColumn(column), value));
        return this;
    }

    public UpdateWrapper<T> eq(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.EQ, column, value);
    }

    public UpdateWrapper<T> ne(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.NE, column, value);
    }

    public UpdateWrapper<T> in(Function<T, ?> column, @Nullable Object values) {
        return addCondition(Operator.IN, column, values);
    }

    public UpdateWrapper<T> addCondition(Condition condition) {
        conditions.add(condition);
        return this;
    }

    public List<SetClause> getSetClauses() {
        return List.copyOf(setClauses);
    }

    public List<Condition> getConditions() {
        return List.copyOf(conditions);
    }

    private UpdateWrapper<T> addCondition(Operator operator, Function<T, ?> column, @Nullable Object value) {
        conditions.add(new Condition(operator, resolveColumn(column), value));
        return this;
    }

    private String resolveColumn(Function<T, ?> column) {
        if (column instanceof QueryWrapper.ResolvableFunction resolvable) {
            return resolvable.getColumnName();
        }
        return column.toString().substring(column.toString().indexOf('$') + 1);
    }

    /**
     * 单个 SET 子句。
     *
     * @param column 列名
     * @param value  值
     * @author 王飞
     * @since 2026-08-24
     */
    public record SetClause(String column, @Nullable Object value) {
    }
}
