package cn.jowen.framework.data.core.exception;

import org.jspecify.annotations.NullMarked;

/**
 * 数据访问超时异常，当操作超出预定时间限制时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class TimeoutException extends TransientDataAccessException {

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
