package cn.jowen.framework.data.core.query;

import cn.jowen.framework.data.core.sort.Order;
import cn.jowen.framework.data.core.sort.Sort;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@NullMarked
public final class QueryWrapper<T> {

    private final List<Condition> conditions = new ArrayList<>();
    private final List<Join<T>> joins = new ArrayList<>();
    private final List<String> groupBy = new ArrayList<>();
    private final List<String> orderBy = new ArrayList<>();
    private @Nullable Integer limit;
    private @Nullable Integer offset;

    public QueryWrapper() {
    }

    public QueryWrapper<T> eq(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.EQ, column, value);
    }

    public QueryWrapper<T> ne(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.NE, column, value);
    }

    public QueryWrapper<T> like(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LIKE, column, value);
    }

    public QueryWrapper<T> likeLeft(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LIKE_LEFT, column, value);
    }

    public QueryWrapper<T> likeRight(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LIKE_RIGHT, column, value);
    }

    public QueryWrapper<T> gt(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.GT, column, value);
    }

    public QueryWrapper<T> gte(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.GTE, column, value);
    }

    public QueryWrapper<T> lt(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LT, column, value);
    }

    public QueryWrapper<T> lte(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LTE, column, value);
    }

    public QueryWrapper<T> in(Function<T, ?> column, @Nullable Object values) {
        return addCondition(Operator.IN, column, values);
    }

    public QueryWrapper<T> notIn(Function<T, ?> column, @Nullable Object values) {
        return addCondition(Operator.NOT_IN, column, values);
    }

    public QueryWrapper<T> between(Function<T, ?> column, Object start, Object end) {
        return addCondition(Operator.BETWEEN, column, new Object[]{start, end});
    }

    public QueryWrapper<T> isNull(Function<T, ?> column) {
        return addCondition(Operator.IS_NULL, column, null);
    }

    public QueryWrapper<T> isNotNull(Function<T, ?> column) {
        return addCondition(Operator.IS_NOT_NULL, column, null);
    }

    public QueryWrapper<T> addCondition(Condition condition) {
        conditions.add(condition);
        return this;
    }

    public QueryWrapper<T> and(QueryWrapper<T> other) {
        conditions.addAll(other.conditions);
        return this;
    }

    public QueryWrapper<T> or(QueryWrapper<T> other) {
        if (!conditions.isEmpty()) {
            conditions.add(new Condition(Operator.AND, "", null));
        }
        conditions.add(new Condition(Operator.OR, "", null));
        conditions.addAll(other.conditions);
        return this;
    }

    public QueryWrapper<T> innerJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        joins.add(new Join<>(JoinType.INNER, left, rightEntity, right));
        return this;
    }

    public QueryWrapper<T> leftJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        joins.add(new Join<>(JoinType.LEFT, left, rightEntity, right));
        return this;
    }

    public QueryWrapper<T> rightJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        joins.add(new Join<>(JoinType.RIGHT, left, rightEntity, right));
        return this;
    }

    @SafeVarargs
    public final QueryWrapper<T> groupBy(Function<T, ?>... columns) {
        for (Function<T, ?> c : columns) {
            groupBy.add(resolveColumn(c));
        }
        return this;
    }

    public QueryWrapper<T> having(Function<T, ?> column, Operator operator, @Nullable Object value) {
        groupBy.add(column.toString() + " " + operator.name());
        return this;
    }

    public QueryWrapper<T> orderByAsc(Function<T, ?> column) {
        orderBy.add(resolveColumn(column) + " ASC");
        return this;
    }

    public QueryWrapper<T> orderByDesc(Function<T, ?> column) {
        orderBy.add(resolveColumn(column) + " DESC");
        return this;
    }

    public QueryWrapper<T> orderBy(Sort sort) {
        for (Order order : sort.getOrders()) {
            orderBy.add(order.toSql());
        }
        return this;
    }

    public QueryWrapper<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    public QueryWrapper<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    public List<Condition> getConditions() {
        return List.copyOf(conditions);
    }

    public List<Join<T>> getJoins() {
        return List.copyOf(joins);
    }

    public List<String> getGroupBy() {
        return List.copyOf(groupBy);
    }

    public List<String> getOrderBy() {
        return List.copyOf(orderBy);
    }

    public @Nullable Integer getLimit() {
        return limit;
    }

    public @Nullable Integer getOffset() {
        return offset;
    }

    private QueryWrapper<T> addCondition(Operator operator, Function<T, ?> column, @Nullable Object value) {
        conditions.add(new Condition(operator, resolveColumn(column), value));
        return this;
    }

    private String resolveColumn(Function<T, ?> column) {
        if (column instanceof ResolvableFunction resolvable) {
            return resolvable.getColumnName();
        }
        return column.toString().substring(column.toString().indexOf('$') + 1);
    }

    public record Join<E>(JoinType type, Function<E, ?> leftColumn, Class<?> rightEntity, Function<?, ?> rightColumn) {
    }

    public interface ResolvableFunction<T, R> extends Function<T, R> {
        String getColumnName();
    }
}
