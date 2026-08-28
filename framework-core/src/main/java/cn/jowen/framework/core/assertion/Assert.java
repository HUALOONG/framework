package cn.jowen.framework.core.assertion;

import cn.jowen.framework.core.exception.BusinessException;
import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

import static cn.jowen.framework.core.util.CollectionUtils.isEmpty;
import static cn.jowen.framework.core.util.StringUtils.isEmpty;

/**
 * 断言工具，用于参数校验与状态校验。校验失败时抛出 {@link BusinessException}（可携带 {@link ErrorCode}）。
 *
 * <p>所有方法均为静态；满足即正常返回，不满足即抛异常。用于替代散落的 {@code if (x == null) throw ...} 模板代码。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Assert {

    private Assert() {
    }

    /**
     * 断言对象非 {@code null}，否则抛出 {@link BusinessException}。
     *
     * @param obj     被校验对象，可为 {@code null}
     * @param code    错误码，可为 {@code null}
     * @param message 错误消息，可为 {@code null}
     */
    public static void notNull(@Nullable Object obj, @Nullable ErrorCode code, @Nullable String message) {
        if (obj == null) {
            fail(code, message != null ? message : (code != null ? code.message() : "对象不能为 null"));
        }
    }

    /**
     * 断言字符串非空（非 {@code null} 且非空白），否则抛出异常。
     *
     * @param str     被校验字符串，可为 {@code null}
     * @param code    错误码，可为 {@code null}
     * @param message 错误消息，可为 {@code null}
     */
    public static void notEmpty(@Nullable String str, @Nullable ErrorCode code, @Nullable String message) {
        if (isEmpty(str)) {
            fail(code, message != null ? message : "字符串不能为空");
        }
    }

    /**
     * 断言集合非空（非 {@code null} 且非空），否则抛出异常。
     *
     * @param coll    被校验集合，可为 {@code null}
     * @param code    错误码，可为 {@code null}
     * @param message 错误消息，可为 {@code null}
     */
    public static void notEmpty(@Nullable Collection<?> coll, @Nullable ErrorCode code, @Nullable String message) {
        if (isEmpty(coll)) {
            fail(code, message != null ? message : "集合不能为空");
        }
    }

    /**
     * 断言 Map 非空（非 {@code null} 且非空），否则抛出异常。
     *
     * @param map     被校验 Map，可为 {@code null}
     * @param code    错误码，可为 {@code null}
     * @param message 错误消息，可为 {@code null}
     */
    public static void notEmpty(@Nullable Map<?, ?> map, @Nullable ErrorCode code, @Nullable String message) {
        if (isEmpty(map)) {
            fail(code, message != null ? message : "Map 不能为空");
        }
    }

    /**
     * 断言表达式为 {@code true}，否则抛出异常。
     *
     * @param expression 布尔表达式
     * @param code       错误码，可为 {@code null}
     * @param message    错误消息，可为 {@code null}
     */
    public static void isTrue(boolean expression, @Nullable ErrorCode code, @Nullable String message) {
        if (!expression) {
            fail(code, message != null ? message : "条件校验失败");
        }
    }

    private static void fail(@Nullable ErrorCode code, @Nullable String message) {
        if (code != null) {
            throw new BusinessException(code, message != null ? message : code.message());
        }
        throw new BusinessException(message != null ? message : "校验失败");
    }
}
