package cn.jowen.framework.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 错误码接口，定义可被框架与业务共享的错误码规范。
 *
 * <p>实现类通常为枚举，提供 {@code code()} 与 {@code message()} 两个维度：
 * {@code code} 用于前端精确识别错误类型，{@code message} 为默认可读信息（支持后续国际化替换）。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface ErrorCode {

    /**
     * 返回错误码，通常为字符串或数字形式的可枚举标识。
     *
     * @return 错误码，不可为 {@code null}
     */
    String code();

    /**
     * 返回错误默认描述信息。
     *
     * @return 默认错误信息，不可为 {@code null}
     */
    String message();
}
