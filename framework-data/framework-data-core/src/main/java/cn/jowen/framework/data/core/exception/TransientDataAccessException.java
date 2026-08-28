package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 瞬时性数据访问异常基类，代表可能随重试成功恢复的异常。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class TransientDataAccessException extends DataAccessException {

    public TransientDataAccessException(String message) {
        super(message);
    }

    public TransientDataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
