package cn.jowen.framework.data.core.query;

import cn.jowen.framework.data.core.page.Sort;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 类型安全条件查询描述。零依赖，供实现层翻译为 SQL（或 ORM 条件）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class Query {

    private final List<Condition> conditions = new ArrayList<>();
    private Sort sort = Sort.unsorted();
    private @Nullable Integer limit;

    private Query() {
    }

    /**
     * 创建空查询。
     *
     * @return 查询
     */
    public static Query empty() {
        return new Query();
    }

    /**
     * 添加等值条件。
     *
     * @param column 列名，不可为 {@code null}
     * @param value  值，可为 {@code null}（转为 IS NULL）
     * @return 当前查询（链式）
     */
    public Query eq(String column, @Nullable Object value) {
        conditions.add(new Condition(Operator.EQ, column, value));
        return this;
    }

    /**
     * 添加不等条件。
     *
     * @param column 列名，不可为 {@code null}
     * @param value  值，可为 {@code null}
     * @return 当前查询（链式）
     */
    public Query ne(String column, @Nullable Object value) {
        conditions.add(new Condition(Operator.NE, column, value));
        return this;
    }

    /**
     * 添加模糊匹配条件（实现层转为 LIKE %value%）。
     *
     * @param column 列名，不可为 {@code null}
     * @param value  值，可为 {@code null}
     * @return 当前查询（链式）
     */
    public Query like(String column, @Nullable Object value) {
        conditions.add(new Condition(Operator.LIKE, column, value));
        return this;
    }

    /**
     * 添加大于条件。
     *
     * @param column 列名，不可为 {@code null}
     * @param value  值，可为 {@code null}
     * @return 当前查询（链式）
     */
    public Query gt(String column, @Nullable Object value) {
        conditions.add(new Condition(Operator.GT, column, value));
        return this;
    }

    /**
     * 设置排序。
     *
     * @param sort 排序，不可为 {@code null}
     * @return 当前查询（链式）
     */
    public Query orderBy(Sort sort) {
        this.sort = sort;
        return this;
    }

    /**
     * 设置结果上限。
     *
     * @param limit 上限，≥0
     * @return 当前查询（链式）
     */
    public Query limit(int limit) {
        this.limit = limit;
        return this;
    }

    public List<Condition> getConditions() {
        return List.copyOf(conditions);
    }

    public Sort getSort() {
        return sort;
    }

    public @Nullable Integer getLimit() {
        return limit;
    }

    /** 比较操作符。 */
    public enum Operator {
        EQ,
        NE,
        LIKE,
        GT,
        LT,
        GTE,
        LTE,
        IN
    }

    /** 单条件描述。 */
    public static final class Condition {
        private final Operator operator;
        private final String column;
        private final @Nullable Object value;

        public Condition(Operator operator, String column, @Nullable Object value) {
            this.operator = operator;
            this.column = column;
            this.value = value;
        }

        public Operator getOperator() {
            return operator;
        }

        public String getColumn() {
            return column;
        }

        public @Nullable Object getValue() {
            return value;
        }
    }
}
