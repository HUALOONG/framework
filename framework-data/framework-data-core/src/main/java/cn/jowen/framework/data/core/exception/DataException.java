package cn.jowen.framework.data.core.exception;

import cn.jowen.framework.core.exception.SystemException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
/**
 * 「DataException」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class DataException extends SystemException {

    /**
     * 构造实例。
     * @param message 参数 message
     */
    public DataException(String message) {
        super(message);
    }

    /**
     * 构造实例。
     * @param message 参数 message
     * @param cause 参数 cause
     */
    public DataException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造实例。
     * @param errorCode 参数 errorCode
     */
    public DataException(cn.jowen.framework.core.exception.ErrorCode errorCode) {
        super(errorCode);
    }

    /**
     * 构造实例。
     * @param errorCode 参数 errorCode
     * @param message 参数 message
     */
    public DataException(cn.jowen.framework.core.exception.ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * 构造实例。
     * @param errorCode 参数 errorCode
     * @param cause 参数 cause
     */
    public DataException(cn.jowen.framework.core.exception.ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    /**
     * 执行of操作。
     * @param message 参数 message
     * @param cause 参数 cause
     * @return 结果
     */
    public static DataException of(String message, @Nullable Throwable cause) {
        return new DataException(message, cause);
    }
}
