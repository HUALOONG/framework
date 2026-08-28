package cn.jowen.framework.data.jdbc.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务同步回调：在事务提交/完成后执行额外逻辑（如清除线程本地资源、发送事件）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface TransactionSynchronization {

    /**
     * 事务提交前回调。
     */
    default void beforeCommit() {
    }

    /**
     * 事务提交成功后回调。
     */
    default void afterCommit() {
    }

    /**
     * 事务完成后（提交或回滚）回调。
     */
    default void afterCompletion() {
    }
}
