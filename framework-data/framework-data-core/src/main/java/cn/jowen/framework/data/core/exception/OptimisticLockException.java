package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 乐观锁失败异常，对应数据库版本字段冲突（如 {@code UPDATE ... WHERE version = ?} 影响行数为 0）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class OptimisticLockException extends DataAccessException {

    public OptimisticLockException(String message) {
        super(message);
    }

    public OptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public OptimisticLockException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
