package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务状态，用于判断是否需要回滚与是否已完成。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface TransactionStatus {

    /**
     * 是否已标记为回滚仅（仅回滚，不提交）。
     *
     * @return 是返回 {@code true}
     */
    boolean isRollbackOnly();

    /**
     * 标记回滚仅。
     */
    void setRollbackOnly();

    /**
     * 事务是否已完成（提交或回滚后）。
     *
     * @return 已完成返回 {@code true}
     */
    boolean isCompleted();
}
