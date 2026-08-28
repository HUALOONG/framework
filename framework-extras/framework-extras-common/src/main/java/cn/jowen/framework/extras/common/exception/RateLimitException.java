package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 接口限流异常，请求超出限流阈值时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class RateLimitException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#RATE_LIMIT_EXCEEDED}。
     *
     * @param message 异常消息
     */
    public RateLimitException(String message) {
        super(ErrorCodeEnum.RATE_LIMIT_EXCEEDED, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public RateLimitException(String message, Throwable cause) {
        super(ErrorCodeEnum.RATE_LIMIT_EXCEEDED, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public RateLimitException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
