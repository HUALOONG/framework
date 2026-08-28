package cn.jowen.framework.core.exception;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 框架异常根类，所有框架自定义异常的父类。
 *
 * <p>统一承载 {@link ErrorCode}，使上层能够以错误码而非异常类型做分支处理。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class FrameworkException extends RuntimeException {

    /**
     * 关联的错误码，可能为 {@code null}（未指定时）。
     */
    private final @Nullable ErrorCode errorCode;

    /**
     * 创建异常。
     *
     * @param message 异常消息
     */
    public FrameworkException(String message) {
        super(message);
        this.errorCode = null;
    }

    /**
     * 创建异常。
     *
     * @param message 异常消息
     * @param cause   异常原因
     */
    public FrameworkException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    /**
     * 创建异常。
     *
     * @param errorCode 错误码
     */
    public FrameworkException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /**
     * 创建异常。
     *
     * @param errorCode 错误码
     * @param cause     异常原因
     */
    public FrameworkException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode;
    }

    /**
     * 创建异常。
     *
     * @param errorCode 错误码
     * @param message   异常消息
     */
    public FrameworkException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 返回关联的错误码。
     *
     * @return 错误码，未指定时返回 {@code null}
     */
    public @Nullable ErrorCode getErrorCode() {
        return errorCode;
    }
}
