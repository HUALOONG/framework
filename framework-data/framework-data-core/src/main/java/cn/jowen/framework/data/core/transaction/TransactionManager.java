package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务管理器抽象。实现层桥接 Spring/JDBC 的事务资源。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface TransactionManager {

    /**
     * 开启事务。
     *
     * @param definition 事务定义，不可为 {@code null}
     * @return 事务状态，不可为 {@code null}
     */
    TransactionStatus begin(TransactionDefinition definition);

    /**
     * 提交事务。
     *
     * @param status 事务状态，不可为 {@code null}
     */
    void commit(TransactionStatus status);

    /**
     * 回滚事务。
     *
     * @param status 事务状态，不可为 {@code null}
     */
    void rollback(TransactionStatus status);
}
