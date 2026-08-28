package cn.jowen.framework.extras.web.lock;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;

/**
 * 获取锁失败异常。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LockAcquireException extends ExtrasException {

    /**
     * 构造实例。
     * @param message 参数 message
     */
    public LockAcquireException(String message) {
        super(message);
    }

    /**
     * 构造实例。
     * @param message 参数 message
     * @param cause 参数 cause
     */
    public LockAcquireException(String message, Throwable cause) {
        super(message, cause);
    }
}
