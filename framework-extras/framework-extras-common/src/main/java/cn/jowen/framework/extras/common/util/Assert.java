package cn.jowen.framework.extras.common.util;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.extras.common.exception.ErrorCodeEnum;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 参数校验断言工具。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Assert {

    private Assert() {
    }

    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new ExtrasException(ErrorCodeEnum.BUSINESS_REJECTED, message);
        }
    }

    public static void isTrue(boolean expression, ErrorCode errorCode) {
        if (!expression) {
            throw new ExtrasException(errorCode);
        }
    }

    public static void notNull(@Nullable Object object, String message) {
        if (object == null) {
            throw new ExtrasException(ErrorCodeEnum.INVALID_ARGUMENT, message);
        }
    }

    public static void hasText(@Nullable String text, String message) {
        if (StringUtils.isBlank(text)) {
            throw new ExtrasException(ErrorCodeEnum.INVALID_ARGUMENT, message);
        }
    }
}
