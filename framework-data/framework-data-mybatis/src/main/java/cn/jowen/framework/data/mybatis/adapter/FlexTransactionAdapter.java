package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.transaction.TransactionCallback;
import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.core.transaction.TransactionTemplate;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.Callable;

@NullMarked
public final class FlexTransactionAdapter implements TransactionManager {

    private static final Logger logger = LoggerFactory.getLogger(FlexTransactionAdapter.class);

    private final Object txManager;

    public FlexTransactionAdapter(Object txManager) {
        if (txManager == null) throw new IllegalArgumentException("txManager must not be null");
        this.txManager = txManager;
    }

    public TransactionTemplate template() {
        return new TransactionTemplate(this);
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        if (definition == null) throw new IllegalArgumentException("definition must not be null");
        FlexTxStatus status = new FlexTxStatus();
        try {
            var method = txManager.getClass().getMethod("setAutoCommit", boolean.class);
            method.invoke(txManager, false);
        } catch (Exception e) {
            logger.debug("事务 begin 反射调用失败，降级为手动管理: " + e.getMessage());
        }
        return status;
    }

    @Override
    public void commit(TransactionStatus status) {
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (status instanceof FlexTxStatus fts) {
            fts.committed = true;
            try {
                var method = txManager.getClass().getMethod("commit");
                method.invoke(txManager);
            } catch (Exception e) {
                logger.error("事务提交失败: " + e.getMessage());
            }
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        if (status == null) throw new IllegalArgumentException("status must not be null");
        if (status instanceof FlexTxStatus fts) {
            fts.committed = true;
            try {
                var method = txManager.getClass().getMethod("rollback");
                method.invoke(txManager);
            } catch (Exception e) {
                logger.error("事务回滚失败: " + e.getMessage());
            }
        }
    }

    public void execute(TransactionCallback callback, String action) {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        if (action == null) throw new IllegalArgumentException("action must not be null");
        TransactionStatus status = begin(TransactionDefinition.defaults());
        try {
            callback.execute(status);
            commit(status);
        } catch (RuntimeException e) {
            rollback(status);
            throw FlexExceptionTranslator.translate(action, null, e);
        }
    }

    public <R> R execute(Callable<R> callback, String action) {
        if (callback == null) throw new IllegalArgumentException("callback must not be null");
        if (action == null) throw new IllegalArgumentException("action must not be null");
        TransactionStatus status = begin(TransactionDefinition.defaults());
        try {
            R result = callback.call();
            commit(status);
            return result;
        } catch (Exception e) {
            rollback(status);
            if (e instanceof RuntimeException re) {
                throw FlexExceptionTranslator.translate(action, null, re);
            }
            throw FlexExceptionTranslator.translate(action, null, new RuntimeException(e));
        }
    }

    private static final class FlexTxStatus implements TransactionStatus {
        private boolean rollbackOnly;
        private boolean committed;

        @Override public boolean isRollbackOnly() { return rollbackOnly; }
        @Override public void setRollbackOnly() { this.rollbackOnly = true; }
        @Override public boolean isCompleted() { return committed; }
    }
}
