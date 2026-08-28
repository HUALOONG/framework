package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 分布式锁异常，获取或释放锁失败时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class LockException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#LOCK_ACQUIRE_FAILED}。
     *
     * @param message 异常消息
     */
    public LockException(String message) {
        super(ErrorCodeEnum.LOCK_ACQUIRE_FAILED, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public LockException(String message, Throwable cause) {
        super(ErrorCodeEnum.LOCK_ACQUIRE_FAILED, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码（如释放失败使用 {@link ErrorCodeEnum#LOCK_RELEASE_FAILED}）。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public LockException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
