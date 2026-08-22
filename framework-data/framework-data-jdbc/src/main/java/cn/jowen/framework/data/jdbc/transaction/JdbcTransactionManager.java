package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * 基于 Spring {@link DataSourceTransactionManager} 的事务管理器实现，桥接 core 事务抽象。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class JdbcTransactionManager implements TransactionManager {

    private final DataSourceTransactionManager delegate;

    public JdbcTransactionManager(DataSource dataSource) {
        this.delegate = new DataSourceTransactionManager(dataSource);
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        DefaultTransactionDefinition def = toSpring(definition);
        org.springframework.transaction.TransactionStatus status = delegate.getTransaction(def);
        return new SpringTransactionStatus(status);
    }

    @Override
    public void commit(TransactionStatus status) {
        delegate.commit(asSpring(status).getDelegate());
    }

    @Override
    public void rollback(TransactionStatus status) {
        delegate.rollback(asSpring(status).getDelegate());
    }

    private DefaultTransactionDefinition toSpring(TransactionDefinition definition) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(mapPropagation(definition.getPropagation()));
        def.setIsolationLevel(mapIsolation(definition.getIsolation()));
        def.setReadOnly(definition.isReadOnly());
        if (definition.getTimeout() >= 0) {
            def.setTimeout(definition.getTimeout());
        }
        return def;
    }

    private int mapPropagation(TransactionDefinition.Propagation p) {
        return switch (p) {
            case REQUIRED -> org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRED;
            case REQUIRES_NEW -> org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            case NESTED -> org.springframework.transaction.TransactionDefinition.PROPAGATION_NESTED;
            case SUPPORTS -> org.springframework.transaction.TransactionDefinition.PROPAGATION_SUPPORTS;
            case NOT_SUPPORTED -> org.springframework.transaction.TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
            case NEVER -> org.springframework.transaction.TransactionDefinition.PROPAGATION_NEVER;
            case MANDATORY -> org.springframework.transaction.TransactionDefinition.PROPAGATION_MANDATORY;
        };
    }

    private int mapIsolation(TransactionDefinition.Isolation i) {
        return switch (i) {
            case DEFAULT -> org.springframework.transaction.TransactionDefinition.ISOLATION_DEFAULT;
            case READ_UNCOMMITTED -> org.springframework.transaction.TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
            case READ_COMMITTED -> org.springframework.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;
            case REPEATABLE_READ -> org.springframework.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;
            case SERIALIZABLE -> org.springframework.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE;
        };
    }

    private SpringTransactionStatus asSpring(TransactionStatus status) {
        if (!(status instanceof SpringTransactionStatus s)) {
            throw new IllegalArgumentException("未知事务状态类型：" + status.getClass());
        }
        return s;
    }

    /** Spring 事务状态的 core 包装。 */
    @NullMarked
    private static final class SpringTransactionStatus implements TransactionStatus {
        private final org.springframework.transaction.TransactionStatus delegate;
        private boolean rollbackOnly;

        private SpringTransactionStatus(org.springframework.transaction.TransactionStatus delegate) {
            this.delegate = delegate;
        }

        private org.springframework.transaction.TransactionStatus getDelegate() {
            return delegate;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly || delegate.isRollbackOnly();
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
            delegate.setRollbackOnly();
        }

        @Override
        public boolean isCompleted() {
            return delegate.isCompleted();
        }
    }
}
