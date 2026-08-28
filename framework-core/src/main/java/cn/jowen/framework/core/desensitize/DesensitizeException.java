package cn.jowen.framework.core.desensitize;

import cn.jowen.framework.core.exception.FrameworkException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 脱敏异常：脱敏配置错误（未知策略、非法保留位数等）时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class DesensitizeException extends FrameworkException {

    /**
     * 构造异常。
     *
     * @param message 错误描述
     */
    public DesensitizeException(@Nullable String message) {
        super(message);
    }

    /**
     * 构造异常。
     *
     * @param message 错误描述
     * @param cause   根因
     */
    public DesensitizeException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
