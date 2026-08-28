package cn.jowen.framework.extras.idempotent;

import cn.jowen.framework.core.exception.BusinessException;
import org.jspecify.annotations.NullMarked;

/**
 * 幂等冲突异常：KEY 模式下检测到重复请求时抛出，归类于业务异常。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public class IdempotencyException extends BusinessException {

    /**
     * 构造异常。
     *
     * @param message 异常消息
     */
    public IdempotencyException(String message) {
        super(message);
    }

    /**
     * 构造异常。
     *
     * @param message 异常消息
     * @param cause   根因
     */
    public IdempotencyException(String message, Throwable cause) {
        super(message, cause);
    }
}