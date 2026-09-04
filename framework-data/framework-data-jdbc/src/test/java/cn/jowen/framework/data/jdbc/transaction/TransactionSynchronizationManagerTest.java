package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link TransactionSynchronizationManager} 生命周期测试：绑定/解绑、回调注册与三阶段触发。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class TransactionSynchronizationManagerTest {

    @AfterEach
    void cleanUp() {
        TransactionSynchronizationManager.unbindResource();
    }

    @Test
    void bindAndUnbind_resourceLifecycle() {
        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isNull();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();

        ConnectionHolder holder = new ConnectionHolder(mock(Connection.class), true);
        TransactionSynchronizationManager.bindResource(holder);

        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isSameAs(holder);
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();

        TransactionSynchronizationManager.unbindResource();
        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isNull();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }

    @Test
    void unbind_clearsRegisteredSynchronizations() {
        AtomicInteger completion = new AtomicInteger();
        TransactionSynchronizationManager.bindResource(
                new ConnectionHolder(mock(Connection.class), false));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion() {
                completion.incrementAndGet();
            }
        });
        assertThat(TransactionSynchronizationManager.getSynchronizations()).hasSize(1);

        TransactionSynchronizationManager.unbindResource();

        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
        // 解绑后不再触发
        TransactionSynchronizationManager.triggerAfterCompletion();
        assertThat(completion.get()).isEqualTo(0);
    }

    @Test
    void trigger_firesAllPhasesInOrder() {
        StringBuilder order = new StringBuilder();
        TransactionSynchronizationManager.bindResource(
                new ConnectionHolder(mock(Connection.class), true));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit() {
                order.append("before,");
            }

            @Override
            public void afterCommit() {
                order.append("after,");
            }

            @Override
            public void afterCompletion() {
                order.append("completion");
            }
        });
        // 无注册时触发不抛异常
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        });

        TransactionSynchronizationManager.triggerBeforeCommit();
        TransactionSynchronizationManager.triggerAfterCommit();
        TransactionSynchronizationManager.triggerAfterCompletion();

        assertThat(order.toString()).isEqualTo("before,after,completion");
        assertThat(TransactionSynchronizationManager.getSynchronizations())
                .hasSize(2)
                .isInstanceOf(List.class);
    }

    @Test
    void getSynchronizations_withoutRegistration_isEmpty() {
        assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
    }
}
