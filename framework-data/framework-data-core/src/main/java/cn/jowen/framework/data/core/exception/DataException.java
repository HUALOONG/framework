package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class DataException extends SystemException {

    public DataException(String message) {
        super(message);
    }

    public DataException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataException(cn.jowen.framework.core.exception.ErrorCode errorCode) {
        super(errorCode);
    }

    public DataException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public DataException(cn.jowen.framework.core.exception.ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public static DataException of(String message, @Nullable Throwable cause) {
        return new DataException(message, cause);
    }
}
