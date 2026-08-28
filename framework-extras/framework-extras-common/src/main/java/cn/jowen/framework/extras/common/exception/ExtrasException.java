package cn.jowen.framework.extras.common.exception;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.core.exception.FrameworkException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 扩展模块统一异常根类，所有 extras 子模块自定义异常应继承本类。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExtrasException extends FrameworkException {

    public ExtrasException(String message) {
        super(message);
    }

    public ExtrasException(String message, Throwable cause) {
        super(message, cause);
    }

    public ExtrasException(ErrorCode errorCode) {
        super(errorCode);
    }

    public ExtrasException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public ExtrasException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
