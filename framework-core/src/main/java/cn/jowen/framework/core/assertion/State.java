package cn.jowen.framework.core.assertion;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * 状态断言工具：校验系统/内部运行状态，失败抛出 {@link SystemException}。
 *
 * <p>与 {@link Assert}（面向业务入参，抛 {@link cn.jowen.framework.core.exception.BusinessException}）互补：
 * 本类面向"系统状态"前置条件，如组件生命周期、内部一致性、环境约束等，
 * 失败属于系统级错误，不应由调用方捕获修复。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class State {

    private State() {
    }

    /**
     * 校验状态表达式。
     *
     * @param expression 必须为 true 的状态表达式
     * @param code       错误码（可为空，空则使用默认系统错误码）
     * @param message    失败消息模板（可为空）
     * @throws SystemException 当 {@code expression} 为 false
     */
    public static void checkState(boolean expression, @Nullable ErrorCode code, @Nullable String message) {
        if (!expression) {
            fail(code, message);
        }
    }

    /**
     * 校验状态表达式（无消息）。
     *
     * @param expression 必须为 true 的状态表达式
     * @throws SystemException 当 {@code expression} 为 false
     */
    public static void checkState(boolean expression) {
        checkState(expression, null, null);
    }

    /**
     * 校验对象非空。
     *
     * @param obj     目标对象（可为 null，null 时失败）
     * @param code    错误码（可为空）
     * @param message 失败消息（可为空）
     * @return 原对象（方便链式使用）
     * @throws SystemException 当 {@code obj} 为 null
     */
    public static <T> T checkNotNull(@Nullable T obj, @Nullable ErrorCode code, @Nullable String message) {
        if (obj == null) {
            fail(code, message);
        }
        return Objects.requireNonNull(obj);
    }

    /**
     * 校验字符串非空（非 null 且去除首尾空白后非空）。
     *
     * @param text    目标字符串
     * @param code    错误码（可为空）
     * @param message 失败消息（可为空）
     * @return 原字符串
     * @throws SystemException 当 {@code text} 为空白
     */
    public static String checkNotEmpty(@Nullable String text, @Nullable ErrorCode code, @Nullable String message) {
        if (text == null || text.isBlank()) {
            fail(code, message);
        }
        return Objects.requireNonNull(text);
    }

    /**
     * 校验集合非空。
     *
     * @param collection 目标集合
     * @param code       错误码（可为空）
     * @param message    失败消息（可为空）
     * @return 原集合
     * @throws SystemException 当 {@code collection} 为 null 或为空
     */
    public static <T extends Collection<?>> T checkNotEmpty(@Nullable T collection, @Nullable ErrorCode code,
                                                            @Nullable String message) {
        if (collection == null || collection.isEmpty()) {
            fail(code, message);
        }
        return Objects.requireNonNull(collection);
    }

    /**
     * 校验 Map 非空。
     *
     * @param map     目标 Map
     * @param code    错误码（可为空）
     * @param message 失败消息（可为空）
     * @return 原 Map
     * @throws SystemException 当 {@code map} 为 null 或为空
     */
    public static <T extends Map<?, ?>> T checkNotEmpty(@Nullable T map, @Nullable ErrorCode code,
                                                        @Nullable String message) {
        if (map == null || map.isEmpty()) {
            fail(code, message);
        }
        return Objects.requireNonNull(map);
    }

    private static void fail(@Nullable ErrorCode code, @Nullable String message) {
        throw code != null
                ? new SystemException(code, message)
                : new SystemException(message != null ? message : "系统状态校验失败");
    }
}
