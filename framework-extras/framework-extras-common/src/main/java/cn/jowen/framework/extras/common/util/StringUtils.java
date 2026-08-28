package cn.jowen.framework.extras.common.util;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 轻量字符串工具类。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class StringUtils {

    private StringUtils() {
    }

    public static boolean isBlank(@Nullable CharSequence cs) {
        if (cs == null) {
            return true;
        }
        int len = cs.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isNotBlank(@Nullable CharSequence cs) {
        return !isBlank(cs);
    }

    public static @Nullable String defaultIfBlank(@Nullable String value, @Nullable String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }
}
