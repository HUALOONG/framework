package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 验证码异常，校验失败或验证码已过期时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class CaptchaException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#CAPTCHA_INVALID}。
     *
     * @param message 异常消息
     */
    public CaptchaException(String message) {
        super(ErrorCodeEnum.CAPTCHA_INVALID, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public CaptchaException(String message, Throwable cause) {
        super(ErrorCodeEnum.CAPTCHA_INVALID, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码（如已过期使用 {@link ErrorCodeEnum#CAPTCHA_EXPIRED}）。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public CaptchaException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
