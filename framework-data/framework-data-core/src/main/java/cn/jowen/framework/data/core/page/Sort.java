package cn.jowen.framework.data.core.page;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 排序定义，支持多字段升/降序。不可变。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class Sort {

    private final List<Order> orders;

    private Sort(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    /**
     * 创建空排序（无排序）。
     *
     * @return 空排序
     */
    public static Sort unsorted() {
        return new Sort(List.of());
    }

    /**
     * 创建单字段升序排序。
     *
     * @param property 字段名，不可为 {@code null}
     * @return 排序
     */
    public static Sort by(String property) {
        return new Sort(List.of(new Order(property, Direction.ASC)));
    }

    /**
     * 创建单字段指定方向排序。
     *
     * @param property 字段名，不可为 {@code null}
     * @param direction 方向，不可为 {@code null}
     * @return 排序
     */
    public static Sort by(String property, Direction direction) {
        return new Sort(List.of(new Order(property, direction)));
    }

    /**
     * 合并另一个排序（追加在末尾）。
     *
     * @param other 其它排序，不可为 {@code null}
     * @return 新排序
     */
    public Sort and(Sort other) {
        List<Order> merged = new ArrayList<>(orders);
        merged.addAll(other.orders);
        return new Sort(merged);
    }

    /**
     * 返回排序条目（不可变）。
     *
     * @return 排序条目列表
     */
    public List<Order> getOrders() {
        return orders;
    }

    /**
     * 是否无排序。
     *
     * @return 无排序返回 {@code true}
     */
    public boolean isEmpty() {
        return orders.isEmpty();
    }

    /** 排序方向。 */
    public enum Direction {
        ASC,
        DESC
    }

    /** 单字段排序条目。 */
    public static final class Order {
        private final String property;
        private final Direction direction;

        public Order(String property, Direction direction) {
            this.property = property;
            this.direction = direction;
        }

        public String getProperty() {
            return property;
        }

        public Direction getDirection() {
            return direction;
        }

        /** 返回本条目反向前缀的 SQL 片段（供方言拼接）。 */
        public String toSql() {
            return property + (direction == Direction.DESC ? " DESC" : " ASC");
        }
    }
}
