package cn.jowen.framework.data.mybatis.exception;

import cn.jowen.framework.data.core.exception.OptimisticLockException;
import org.jspecify.annotations.NullMarked;

/**
 * 乐观锁冲突异常：{@code @Version} 字段更新影响行数为 0 时抛出。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class FlexOptimisticLockException extends OptimisticLockException {

    public FlexOptimisticLockException(String message) {
        super(message);
    }

    public FlexOptimisticLockException(String message, Throwable cause) {
        super(message, cause);
    }
}