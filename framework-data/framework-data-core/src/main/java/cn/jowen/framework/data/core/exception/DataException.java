package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据访问异常基类，归类于框架系统异常。
 *
 * @author Jowen
 * @date 2026-08-21
 */
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

    /**
     * 将底层（如 JDBC/SQL）异常包装为本框架异常。
     *
     * @param message 描述，不可为 {@code null}
     * @param cause   底层异常，可为 {@code null}
     * @return 包装后的异常
     */
    public static DataException of(String message, @Nullable Throwable cause) {
        return new DataException(message, cause);
    }
}
