package cn.jowen.framework.data.core.mapping;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 类型处理器注册中心，按 Java 类型注册 {@link TypeHandler}，供实现层查询并使用。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class TypeHandlerRegistry {

    private final java.util.Map<Class<?>, TypeHandler<?>> handlers = new java.util.HashMap<>();

    /**
     * 注册类型处理器。
     *
     * @param type      Java 类型，不可为 {@code null}
     * @param handler   处理器，不可为 {@code null}
     * @param <T>       类型参数
     */
    public <T> void register(Class<T> type, TypeHandler<T> handler) {
        handlers.put(type, handler);
    }

    /**
     * 按 Java 类型查找类型处理器。
     *
     * @param type 类型，不可为 {@code null}
     * @return 类型处理器，未注册时返回 {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> get(Class<T> type) {
        return (TypeHandler<T>) handlers.get(type);
    }

    /**
     * 根据值类型查找类型处理器。
     *
     * @param value 值，可为 {@code null}
     * @return 类型处理器，可为 {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> get(@Nullable T value) {
        if (value == null) {
            return null;
        }
        return (TypeHandler<T>) handlers.get(value.getClass());
    }
}
