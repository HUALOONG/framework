package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 数据权限异常，行级权限规则改写或校验失败时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class DataPermissionException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#DATA_PERMISSION_DENIED}。
     *
     * @param message 异常消息
     */
    public DataPermissionException(String message) {
        super(ErrorCodeEnum.DATA_PERMISSION_DENIED, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public DataPermissionException(String message, Throwable cause) {
        super(ErrorCodeEnum.DATA_PERMISSION_DENIED, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public DataPermissionException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
