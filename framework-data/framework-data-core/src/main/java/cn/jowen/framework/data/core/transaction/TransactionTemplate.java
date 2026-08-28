package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 编程式事务模板，封装事务开启、提交和回滚的标准流程。
 *
 * <p>调用方只需提供 {@link TransactionCallback} 执行业务逻辑，
 * 模板负责管理事务边界：成功则提交，发生 {@link RuntimeException} 则回滚并重抛。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class TransactionTemplate {

    private final TransactionManager transactionManager;
    private TransactionDefinition definition;

    /**
     * 构造方法。
     *
     * @param transactionManager 事务管理器，不可为 {@code null}
     */
    public TransactionTemplate(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
        this.definition = TransactionDefinition.defaults();
    }

    /**
     * 设置事务定义。
     *
     * @param definition 事务定义，不可为 {@code null}
     */
    public void setTransactionDefinition(TransactionDefinition definition) {
        this.definition = definition;
    }

    /**
     * 在当前事务模板中执行回调。
     *
     * <p>流程：开启事务 → 执行回调 → 提交事务；
     * 若回调抛出 {@link RuntimeException}，则回滚事务并重抛异常。
     *
     * @param callback 业务回调，不可为 {@code null}
     */
    public void execute(TransactionCallback callback) {
        TransactionStatus status = transactionManager.begin(definition);
        try {
            callback.execute(status);
            transactionManager.commit(status);
        } catch (RuntimeException e) {
            transactionManager.rollback(status);
            throw e;
        }
    }
}
