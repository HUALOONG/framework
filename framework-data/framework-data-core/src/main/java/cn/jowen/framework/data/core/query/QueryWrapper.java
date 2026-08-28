package cn.jowen.framework.data.core.query;

import cn.jowen.framework.data.core.sort.Order;
import cn.jowen.framework.data.core.sort.Sort;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@NullMarked
/**
 * 「QueryWrapper」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public final class QueryWrapper<T> {

    /** conditions 不可变字段。 */
    private final List<Condition> conditions = new ArrayList<>();
    /** joins 不可变字段。 */
    private final List<Join<T>> joins = new ArrayList<>();
    /** groupBy 不可变字段。 */
    private final List<String> groupBy = new ArrayList<>();
    /** orderBy 不可变字段。 */
    private final List<String> orderBy = new ArrayList<>();
    /** limit 字段。 */
    private @Nullable Integer limit;
    /** offset 字段。 */
    private @Nullable Integer offset;

    /**
     * 构造实例。
     */
    public QueryWrapper() {
    }

    /**
     * 执行eq操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> eq(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.EQ, column, value);
    }

    /**
     * 执行ne操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> ne(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.NE, column, value);
    }

    /**
     * 执行like操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> like(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LIKE, column, value);
    }

    /**
     * 执行like left操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> likeLeft(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LIKE_LEFT, column, value);
    }

    /**
     * 执行like right操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> likeRight(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LIKE_RIGHT, column, value);
    }

    /**
     * 执行gt操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> gt(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.GT, column, value);
    }

    /**
     * 执行gte操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> gte(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.GTE, column, value);
    }

    /**
     * 执行lt操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> lt(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LT, column, value);
    }

    /**
     * 执行lte操作。
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> lte(Function<T, ?> column, @Nullable Object value) {
        return addCondition(Operator.LTE, column, value);
    }

    /**
     * 执行in操作。
     * @param values 参数 values
     * @return 结果
     */
    public QueryWrapper<T> in(Function<T, ?> column, @Nullable Object values) {
        return addCondition(Operator.IN, column, values);
    }

    /**
     * 执行not in操作。
     * @param values 参数 values
     * @return 结果
     */
    public QueryWrapper<T> notIn(Function<T, ?> column, @Nullable Object values) {
        return addCondition(Operator.NOT_IN, column, values);
    }

    /**
     * 执行between操作。
     * @param start 参数 start
     * @param end 参数 end
     * @return 结果
     */
    public QueryWrapper<T> between(Function<T, ?> column, Object start, Object end) {
        return addCondition(Operator.BETWEEN, column, new Object[]{start, end});
    }

    /**
     * 获取null。
     * @return 结果
     */
    public QueryWrapper<T> isNull(Function<T, ?> column) {
        return addCondition(Operator.IS_NULL, column, null);
    }

    /**
     * 获取not null。
     * @return 结果
     */
    public QueryWrapper<T> isNotNull(Function<T, ?> column) {
        return addCondition(Operator.IS_NOT_NULL, column, null);
    }

    /**
     * 设置condition。
     * @param condition 参数 condition
     * @return 结果
     */
    public QueryWrapper<T> addCondition(Condition condition) {
        conditions.add(condition);
        return this;
    }

    /**
     * 执行and操作。
     * @return 结果
     */
    public QueryWrapper<T> and(QueryWrapper<T> other) {
        conditions.addAll(other.conditions);
        return this;
    }

    /**
     * 执行or操作。
     * @return 结果
     */
    public QueryWrapper<T> or(QueryWrapper<T> other) {
        if (!conditions.isEmpty()) {
            conditions.add(new Condition(Operator.AND, "", null));
        }
        conditions.add(new Condition(Operator.OR, "", null));
        conditions.addAll(other.conditions);
        return this;
    }

    /**
     * 执行inner join操作。
     * @return 结果
     */
    public QueryWrapper<T> innerJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        joins.add(new Join<>(JoinType.INNER, left, rightEntity, right));
        return this;
    }

    /**
     * 执行left join操作。
     * @return 结果
     */
    public QueryWrapper<T> leftJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        joins.add(new Join<>(JoinType.LEFT, left, rightEntity, right));
        return this;
    }

    /**
     * 执行right join操作。
     * @return 结果
     */
    public QueryWrapper<T> rightJoin(Function<T, ?> left, Class<?> rightEntity, Function<?, ?> right) {
        joins.add(new Join<>(JoinType.RIGHT, left, rightEntity, right));
        return this;
    }

    /**
     * 执行group by操作。
     * @return 结果
     */
    @SafeVarargs
    public final QueryWrapper<T> groupBy(Function<T, ?>... columns) {
        for (Function<T, ?> c : columns) {
            groupBy.add(resolveColumn(c));
        }
        return this;
    }

    /**
     * 执行having操作。
     * @param operator 参数 operator
     * @param value 参数 value
     * @return 结果
     */
    public QueryWrapper<T> having(Function<T, ?> column, Operator operator, @Nullable Object value) {
        groupBy.add(column.toString() + " " + operator.name());
        return this;
    }

    /**
     * 执行order by asc操作。
     * @return 结果
     */
    public QueryWrapper<T> orderByAsc(Function<T, ?> column) {
        orderBy.add(resolveColumn(column) + " ASC");
        return this;
    }

    /**
     * 执行order by desc操作。
     * @return 结果
     */
    public QueryWrapper<T> orderByDesc(Function<T, ?> column) {
        orderBy.add(resolveColumn(column) + " DESC");
        return this;
    }

    /**
     * 执行order by操作。
     * @param sort 参数 sort
     * @return 结果
     */
    public QueryWrapper<T> orderBy(Sort sort) {
        for (Order order : sort.getOrders()) {
            orderBy.add(order.toSql());
        }
        return this;
    }

    /**
     * 执行limit操作。
     * @param limit 参数 limit
     * @return 结果
     */
    public QueryWrapper<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * 执行offset操作。
     * @param offset 参数 offset
     * @return 结果
     */
    public QueryWrapper<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    /**
     * 获取conditions。
     * @return 结果
     */
    public List<Condition> getConditions() {
        return List.copyOf(conditions);
    }

    /**
     * 获取joins。
     * @return 结果
     */
    public List<Join<T>> getJoins() {
        return List.copyOf(joins);
    }

    /**
     * 获取group by。
     * @return 结果
     */
    public List<String> getGroupBy() {
        return List.copyOf(groupBy);
    }

    /**
     * 获取order by。
     * @return 结果
     */
    public List<String> getOrderBy() {
        return List.copyOf(orderBy);
    }

    /**
     * 获取limit。
     * @return 结果
     */
    public @Nullable Integer getLimit() {
        return limit;
    }

    /**
     * 获取offset。
     * @return 结果
     */
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

    /**
     * 「Join」不可变数据载体。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public record Join<E>(JoinType type, Function<E, ?> leftColumn, Class<?> rightEntity, Function<?, ?> rightColumn) {
    }

    /**
     * 「ResolvableFunction」接口定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public interface ResolvableFunction<T, R> extends Function<T, R> {
        String getColumnName();
    }
}
