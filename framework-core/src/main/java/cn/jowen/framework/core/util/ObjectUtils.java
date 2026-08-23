package cn.jowen.framework.core.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 对象判空与默认值工具。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class ObjectUtils {

    private ObjectUtils() {
    }

    /**
     * 当对象为 {@code null} 时返回默认值。
     *
     * @param obj        原对象，可为 {@code null}
     * @param defaultObj 默认值，可为 {@code null}
     * @param <T>        类型
     * @return 原对象或默认值
     */
    public static <T> @Nullable T defaultIfNull(@Nullable T obj, @Nullable T defaultObj) {
        return obj != null ? obj : defaultObj;
    }

    /**
     * 当对象为 {@code null} 时通过供应器获取默认值（惰性求值）。
     *
     * @param obj      原对象，可为 {@code null}
     * @param supplier 默认值供应器，不可为 {@code null}
     * @param <T>      类型
     * @return 原对象或默认值
     */
    public static <T> T defaultIfNull(@Nullable T obj, Supplier<? extends T> supplier) {
        return obj != null ? obj : supplier.get();
    }

    /**
     * 返回对象的哈希码，{@code null} 安全。
     *
     * @param obj 对象，可为 {@code null}
     * @return 哈希码
     */
    public static int nullSafeHashCode(@Nullable Object obj) {
        return Objects.hashCode(obj);
    }
}
