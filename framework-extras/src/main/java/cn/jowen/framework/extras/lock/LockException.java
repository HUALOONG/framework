package cn.jowen.framework.extras.lock;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 分布式锁异常，归类于业务异常。
 */
@NullMarked
public class LockException extends BusinessException {

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public LockException(cn.jowen.framework.core.exception.ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public static LockException of(String message, @Nullable Throwable cause) {
        return new LockException(message, cause);
    }
}