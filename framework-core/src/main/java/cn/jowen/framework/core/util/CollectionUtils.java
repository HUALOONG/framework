package cn.jowen.framework.core.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

/**
 * 集合判空与基础操作工具。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class CollectionUtils {

    private CollectionUtils() {
    }

    /**
     * 判断集合是否为 {@code null} 或空。
     *
     * @param coll 集合，可为 {@code null}
     * @return 为空或 {@code null} 时返回 {@code true}
     */
    public static boolean isEmpty(@Nullable Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * 判断集合是否非空。
     *
     * @param coll 集合，可为 {@code null}
     * @return 非 {@code null} 且非空时返回 {@code true}
     */
    public static boolean isNotEmpty(@Nullable Collection<?> coll) {
        return !isEmpty(coll);
    }

    /**
     * 判断 Map 是否为 {@code null} 或空。
     *
     * @param map Map，可为 {@code null}
     * @return 为空或 {@code null} 时返回 {@code true}
     */
    public static boolean isEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断 Map 是否非空。
     *
     * @param map Map，可为 {@code null}
     * @return 非 {@code null} 且非空时返回 {@code true}
     */
    public static boolean isNotEmpty(@Nullable Map<?, ?> map) {
        return !isEmpty(map);
    }
}
