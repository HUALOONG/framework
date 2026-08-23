package cn.jowen.framework.data.core.transaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link TransactionTemplate} 测试。
 */
class TransactionTemplateTest {

    @Test
    void execute_success() {
        TransactionManager manager = new TransactionManager() {
            @Override public TransactionStatus begin(TransactionDefinition definition) {
                return new TestStatus();
            }
            @Override public void commit(TransactionStatus status) {
            }
            @Override public void rollback(TransactionStatus status) {
            }
        };

        TransactionTemplate template = new TransactionTemplate(manager);
        final boolean[] executed = {false};

        template.execute(status -> {
            executed[0] = true;
        });

        assertThat(executed[0]).isTrue();
    }

    @Test
    void execute_rollbackOnException() {
        boolean[] commitCalled = {false};
        boolean[] rollbackCalled = {false};

        TransactionManager manager = new TransactionManager() {
            @Override public TransactionStatus begin(TransactionDefinition definition) {
                return new TestStatus();
            }
            @Override public void commit(TransactionStatus status) {
                commitCalled[0] = true;
            }
            @Override public void rollback(TransactionStatus status) {
                rollbackCalled[0] = true;
            }
        };

        TransactionTemplate template = new TransactionTemplate(manager);

        assertThatThrownBy(() -> template.execute(status -> {
            throw new RuntimeException("business error");
        })).isInstanceOf(RuntimeException.class)
           .hasMessageContaining("business error");

        assertThat(commitCalled[0]).isFalse();
        assertThat(rollbackCalled[0]).isTrue();
    }

    static class TestStatus implements TransactionStatus {
        private boolean rollbackOnly;

        @Override public boolean isRollbackOnly() { return rollbackOnly; }

        @Override public void setRollbackOnly() { this.rollbackOnly = true; }

        @Override public boolean isCompleted() { return false; }
    }
}
