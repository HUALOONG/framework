package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 死锁异常，当数据库操作因死锁被引擎回滚时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class DeadlockException extends TransientDataAccessException {

    public DeadlockException(String message) {
        super(message);
    }

    public DeadlockException(String message, Throwable cause) {
        super(message, cause);
    }
}
