package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务回调，供 {@link TransactionTemplate} 在事务边界内执行业务逻辑。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@FunctionalInterface
@NullMarked
public interface TransactionCallback {

    /**
     * 在事务上下文中执行业务操作。
     *
     * @param status 当前事务状态，不可为 {@code null}
     */
    void execute(TransactionStatus status);
}
