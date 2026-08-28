package cn.jowen.framework.extras.web.exception;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;

/**
 * Web 扩展模块统一异常。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class WebException extends ExtrasException {
    public WebException(String message) {
        super(message);
    }

    public WebException(String message, Throwable cause) {
        super(message, cause);
    }
}
