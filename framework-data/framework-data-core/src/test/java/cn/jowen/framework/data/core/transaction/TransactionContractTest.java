package cn.jowen.framework.data.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 接口契约测试：用轻量 fake 验证 {@link TransactionManager} 与 {@link TransactionStatus} 的形态自洽。
 */
class TransactionContractTest {

    static final class FakeTransactionStatus implements TransactionStatus {
        private boolean rollbackOnly = false;
        private boolean completed = false;

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        @Override
        public boolean isCompleted() {
            return completed;
        }

        void markCompleted() {
            this.completed = true;
        }
    }

    static final class FakeTransactionManager implements TransactionManager {
        private final List<FakeTransactionStatus> begun = new ArrayList<>();

        @Override
        public TransactionStatus begin(TransactionDefinition definition) {
            FakeTransactionStatus status = new FakeTransactionStatus();
            begun.add(status);
            return status;
        }

        @Override
        public void commit(TransactionStatus status) {
            ((FakeTransactionStatus) status).markCompleted();
        }

        @Override
        public void rollback(TransactionStatus status) {
            ((FakeTransactionStatus) status).markCompleted();
        }
    }

    @Test
    void beginReturnsActiveStatus() {
        FakeTransactionManager manager = new FakeTransactionManager();
        TransactionStatus status = manager.begin(TransactionDefinition.defaults());
        assertThat(status.isCompleted()).isFalse();
        assertThat(status.isRollbackOnly()).isFalse();
    }

    @Test
    void setRollbackOnlyFlipsFlag() {
        FakeTransactionManager manager = new FakeTransactionManager();
        TransactionStatus status = manager.begin(TransactionDefinition.defaults());
        assertThat(status.isRollbackOnly()).isFalse();
        status.setRollbackOnly();
        assertThat(status.isRollbackOnly()).isTrue();
    }

    @Test
    void commitMarksCompleted() {
        FakeTransactionManager manager = new FakeTransactionManager();
        TransactionStatus status = manager.begin(TransactionDefinition.defaults());
        manager.commit(status);
        assertThat(status.isCompleted()).isTrue();
    }

    @Test
    void rollbackMarksCompleted() {
        FakeTransactionManager manager = new FakeTransactionManager();
        TransactionStatus status = manager.begin(TransactionDefinition.defaults());
        manager.rollback(status);
        assertThat(status.isCompleted()).isTrue();
    }
}
