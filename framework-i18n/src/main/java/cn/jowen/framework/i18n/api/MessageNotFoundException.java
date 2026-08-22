package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;

/**
 * 消息未找到异常。在需要严格模式（找不到编码即抛错）的场景抛出，例如
 * {@code MessageSource.getMessageRequired}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class MessageNotFoundException extends I18nException {

    /** 未找到的消息编码。 */
    private final String code;

    public MessageNotFoundException(String code) {
        super(I18nErrorCode.MESSAGE_NOT_FOUND, "消息未找到: " + code);
        this.code = code;
    }

    /** 返回未找到的消息编码。 */
    public String getCode() {
        return code;
    }
}
