package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 幂等校验异常，识别为重复请求时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class IdempotencyException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#DUPLICATE_REQUEST}。
     *
     * @param message 异常消息
     */
    public IdempotencyException(String message) {
        super(ErrorCodeEnum.DUPLICATE_REQUEST, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public IdempotencyException(String message, Throwable cause) {
        super(ErrorCodeEnum.DUPLICATE_REQUEST, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public IdempotencyException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
