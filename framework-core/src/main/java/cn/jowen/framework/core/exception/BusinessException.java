package cn.jowen.framework.core.exception;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 业务异常，表示可预期、可向前端展示的业务错误（如参数非法、状态冲突）。
 *
 * <p>与 {@link SystemException} 区分：业务异常信息通常可安全暴露给调用方；系统异常应被收敛后返回通用错误。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class BusinessException extends FrameworkException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 返回业务错误码（如果存在）。
     *
     * @return 错误码，可能为 {@code null}
     */
    public @Nullable ErrorCode getErrorCode() {
        return (ErrorCode) super.getErrorCode();
    }
}
