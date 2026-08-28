package cn.jowen.framework.core.context;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 上下文键：以"名称 + 值类型"唯一定位一个上下文条目。
 *
 * <p>推荐声明为静态常量并全局共享：
 * <pre>{@code
 * public static final ContextKey<String> TENANT_ID = ContextKey.named("tenantId", String.class);
 * }</pre>
 *
 * @param <T> 值的类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class ContextKey<T> {
    /**
     * 键名称。
     */
    private final String name;

    /**
     * 值类型。
     */
    private final Class<T> type;

    private ContextKey(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * 创建上下文键。
     *
     * @param name 唯一名称（不能为 null/空白）
     * @param type 值类型（不能为 null）
     * @param <T>  值类型参数
     * @return 上下文键
     */
    public static <T> ContextKey<T> named(String name, Class<T> type) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return new ContextKey<>(name, type);
    }

    /**
     * 键名称。
     *
     * @return 键名称
     */
    public String name() {
        return name;
    }

    /**
     * 值类型。
     *
     * @return 值类型
     */
    public Class<T> type() {
        return type;
    }

    /**
     * 类型安全转换：值类型匹配时返回强转结果，不匹配时返回 {@code null}。
     *
     * @param value 待转换值（可为 null）
     * @return 强转后的值或 null
     */
    public @Nullable T cast(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        return type.isInstance(value) ? type.cast(value) : null;
    }

    /**
     * 比较两个上下文键是否相等：名称 + 值类型相同。
     *
     * @param o 待比较对象
     * @return 是否相等
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContextKey<?> that)) {
            return false;
        }
        return name.equals(that.name) && type.equals(that.type);
    }

    /**
     * 计算哈希值：名称 + 值类型。
     *
     * @return 哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    /**
     * 字符串表示：名称。
     *
     * @return 名称
     */
    @Override
    public String toString() {
        return name;
    }
}
