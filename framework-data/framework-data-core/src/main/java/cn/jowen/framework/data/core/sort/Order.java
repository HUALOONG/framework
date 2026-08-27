package cn.jowen.framework.data.core.sort;

import org.jspecify.annotations.NullMarked;

/**
 * 排序字段项：属性名 + 方向。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class Order {

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

    /**
     * 转为 SQL 排序片段（如 {@code name ASC}）。
     *
     * @return SQL 片段
     */
    public String toSql() {
        return property + (direction == Direction.DESC ? " DESC" : " ASC");
    }
}