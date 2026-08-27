package cn.jowen.framework.data.core.sort;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.List;

/**
 * 排序定义，支持多字段升/降序。不可变。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class Sort {

    private final List<Order> orders;

    private Sort(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    public static Sort unsorted() {
        return new Sort(List.of());
    }

    public static Sort by(String property) {
        return new Sort(List.of(new Order(property, Direction.ASC)));
    }

    public static Sort by(String property, Direction direction) {
        return new Sort(List.of(new Order(property, direction)));
    }

    public Sort and(Sort other) {
        List<Order> merged = new ArrayList<>(orders);
        merged.addAll(other.orders);
        return new Sort(merged);
    }

    public List<Order> getOrders() {
        return orders;
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }
}