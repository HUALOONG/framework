package cn.jowen.framework.data.core.exception;

/**
 * 数据访问异常基类，涵盖所有数据访问层的通用异常。
 *
 * @author 王飞
 * @since 2026-08-25
 */
public class DataAccessException extends DataException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataAccessException(cn.jowen.framework.core.exception.ErrorCode errorCode) {
        super(errorCode);
    }

    public DataAccessException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DataAccessException(cn.jowen.framework.core.exception.ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
