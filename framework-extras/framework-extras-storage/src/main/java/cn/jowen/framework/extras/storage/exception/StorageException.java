package cn.jowen.framework.extras.storage.exception;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;

/**
 * 存储模块统一异常。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class StorageException extends ExtrasException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
