package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 唯一键冲突异常，对应数据库 UNIQUE constraint 违规。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public class DuplicateKeyException extends DataAccessException {

    public DuplicateKeyException(String message) {
        super(message);
    }

    public DuplicateKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateKeyException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
