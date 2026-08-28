package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 数据完整性破坏异常，当数据操作违反数据库完整性约束（非唯一键）时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class DataIntegrityViolationException extends DataAccessException {

    public DataIntegrityViolationException(String message) {
        super(message);
    }

    public DataIntegrityViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
