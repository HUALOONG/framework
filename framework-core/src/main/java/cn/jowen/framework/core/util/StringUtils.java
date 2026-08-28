package cn.jowen.framework.core.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 字符串判空与基础操作工具。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class StringUtils {

    private StringUtils() {
    }

    /**
     * 判断字符串是否为 {@code null}、空串或仅含空白字符。
     *
     * @param str 字符串，可为 {@code null}
     * @return 为空时返回 {@code true}
     */
    public static boolean isEmpty(@Nullable String str) {
        return str == null || str.isBlank();
    }

    /**
     * 判断字符串是否非空（非 {@code null} 且含非空白字符）。
     *
     * @param str 字符串，可为 {@code null}
     * @return 非空时返回 {@code true}
     */
    public static boolean isNotEmpty(@Nullable String str) {
        return !isEmpty(str);
    }

    /**
     * 判断两个字符串是否相等（兼容 {@code null}）。
     *
     * @param a 字符串 A，可为 {@code null}
     * @param b 字符串 B，可为 {@code null}
     * @return 相等时返回 {@code true}
     */
    public static boolean equals(@Nullable String a, @Nullable String b) {
        return Objects.equals(a, b);
    }
}
