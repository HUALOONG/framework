package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;

/**
 * 消息格式化异常。参数化消息经格式化器（JavaText / NamedParameter / ICU）处理失败时抛出，
 * 通常源于参数数量不匹配、占位符语法错误或类型不兼容。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class FormatException extends I18nException {

    public FormatException(String message) {
        super(I18nErrorCode.FORMAT_FAILED, message);
    }

    public FormatException(String message, Throwable cause) {
        super(I18nErrorCode.FORMAT_FAILED, message, cause);
    }
}
