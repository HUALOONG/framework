package cn.jowen.framework.core.exception;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 系统异常，表示框架或运行环境内部错误（如配置错误、不可用资源），不应对外暴露细节。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class SystemException extends FrameworkException {

    public SystemException(String message) {
        super(message);
    }

    public SystemException(String message, Throwable cause) {
        super(message, cause);
    }

    public SystemException(ErrorCode errorCode) {
        super(errorCode);
    }

    public SystemException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public SystemException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 返回系统错误码（如果存在）。
     *
     * @return 错误码，可能为 {@code null}
     */
    public @Nullable ErrorCode getErrorCode() {
        return (ErrorCode) super.getErrorCode();
    }
}
