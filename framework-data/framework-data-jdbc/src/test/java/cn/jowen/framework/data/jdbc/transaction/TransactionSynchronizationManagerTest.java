package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link TransactionSynchronizationManager} 测试。
 */
class TransactionSynchronizationManagerTest {

    @AfterEach
    void cleanUp() {
        TransactionSynchronizationManager.unbindResource();
    }

    @Test
    void initiallyInactive() {
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isNull();
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }

    @Test
    void bindAndUnbind() {
        ConnectionHolder holder = new ConnectionHolder(mock(Connection.class), true);
        TransactionSynchronizationManager.bindResource(holder);
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isSameAs(holder);

        TransactionSynchronizationManager.unbindResource();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isNull();
    }

    @Test
    void registerAndTrigger() {
        AtomicInteger beforeCommit = new AtomicInteger();
        AtomicInteger afterCommit = new AtomicInteger();
        AtomicInteger afterCompletion = new AtomicInteger();
        TransactionSynchronization sync = new TransactionSynchronization() {
            @Override
            public void beforeCommit() {
                beforeCommit.incrementAndGet();
            }

            @Override
            public void afterCommit() {
                afterCommit.incrementAndGet();
            }

            @Override
            public void afterCompletion() {
                afterCompletion.incrementAndGet();
            }
        };
        TransactionSynchronizationManager.registerSynchronization(sync);
        TransactionSynchronizationManager.registerSynchronization(sync);

        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(2);

        TransactionSynchronizationManager.triggerBeforeCommit();
        TransactionSynchronizationManager.triggerAfterCommit();
        TransactionSynchronizationManager.triggerAfterCompletion();

        assertThat(beforeCommit.get()).isEqualTo(2);
        assertThat(afterCommit.get()).isEqualTo(2);
        assertThat(afterCompletion.get()).isEqualTo(2);
    }

    @Test
    void unbind_clearsSynchronizations() {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        });
        TransactionSynchronizationManager.unbindResource();
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }
}
