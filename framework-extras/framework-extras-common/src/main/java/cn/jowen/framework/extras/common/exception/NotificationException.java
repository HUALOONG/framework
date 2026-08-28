package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 消息通知异常，渠道发送失败或渠道未注册时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class NotificationException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#NOTIFICATION_SEND_FAILED}。
     *
     * @param message 异常消息
     */
    public NotificationException(String message) {
        super(ErrorCodeEnum.NOTIFICATION_SEND_FAILED, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public NotificationException(String message, Throwable cause) {
        super(ErrorCodeEnum.NOTIFICATION_SEND_FAILED, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public NotificationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
