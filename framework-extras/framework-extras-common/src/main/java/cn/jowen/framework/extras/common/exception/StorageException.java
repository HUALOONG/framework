package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import org.jspecify.annotations.NullMarked;

/**
 * 文件存储异常，上传、下载、删除或生成访问链接失败时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class StorageException extends ExtrasException {

    /**
     * 创建异常，使用默认错误码 {@link ErrorCodeEnum#STORAGE_ERROR}。
     *
     * @param message 异常消息
     */
    public StorageException(String message) {
        super(ErrorCodeEnum.STORAGE_ERROR, message);
    }

    /**
     * 创建异常，使用默认错误码并保留原始异常。
     *
     * @param message 异常消息
     * @param cause   原始异常
     */
    public StorageException(String message, Throwable cause) {
        super(ErrorCodeEnum.STORAGE_ERROR, message);
        initCause(cause);
    }

    /**
     * 创建异常，指定错误码（如文件不存在使用 {@link ErrorCodeEnum#FILE_NOT_FOUND}）。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public StorageException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
