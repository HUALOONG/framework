package cn.jowen.framework.extras.exception;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 限流异常。当请求被限流器拒绝时抛出。
 */
@NullMarked
public class RateLimitException extends BusinessException {

    public RateLimitException(String message) {
        super(message);
    }

    public RateLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public RateLimitException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public RateLimitException(cn.jowen.framework.core.exception.ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public static RateLimitException of(String message, @Nullable Throwable cause) {
        return new RateLimitException(message, cause);
    }
}