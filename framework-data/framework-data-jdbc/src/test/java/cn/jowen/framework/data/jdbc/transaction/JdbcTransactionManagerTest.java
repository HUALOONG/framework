package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.transaction.Isolation;
import cn.jowen.framework.data.core.transaction.Propagation;
import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link JdbcTransactionManager} 测试：传播行为、提交/回滚与异常分支。
 */
class JdbcTransactionManagerTest {

    private final ConnectionProvider provider = mock(ConnectionProvider.class);
    private final Connection real = mock(Connection.class);
    private final JdbcTransactionManager manager = new JdbcTransactionManager(provider);

    @AfterEach
    void cleanUp() throws SQLException {
        TransactionSynchronizationManager.unbindResource();
    }

    private void stubConnection() throws SQLException {
        when(provider.getConnection()).thenReturn(real);
    }

    private static TransactionDefinition def(Propagation propagation) {
        return new TransactionDefinition(propagation, Isolation.DEFAULT, -1, false);
    }

    /** {@link TransactionManager#begin} 返回接口类型，此处还原为具体实现以便访问连接。 */
    private JdbcTransaction beginTx(TransactionDefinition definition) {
        return (JdbcTransaction) manager.begin(definition);
    }

    // ===================== begin 传播 =====================

    @Test
    void begin_required_startsNewTransaction() throws SQLException {
        stubConnection();
        JdbcTransaction status = beginTx(def(Propagation.REQUIRED));

        verify(real).setAutoCommit(false);
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
        assertThat(status.getConnection()).isNotNull();
        manager.commit(status);
    }

    @Test
    void begin_required_joinsExisting() throws SQLException {
        stubConnection();
        JdbcTransaction outer = beginTx(def(Propagation.REQUIRED));
        JdbcTransaction inner = beginTx(def(Propagation.REQUIRED));

        // 仅外层 begin 设置一次 autoCommit，内层 join 不再重复设置
        verify(real, times(1)).setAutoCommit(false);
        assertThat(inner.getConnection()).isSameAs(outer.getConnection());
        manager.rollback(inner);
        manager.commit(outer);
    }

    @Test
    void begin_requiresNew_suspendsExisting() throws SQLException {
        stubConnection();
        JdbcTransaction outer = beginTx(def(Propagation.REQUIRED));
        JdbcTransaction inner = beginTx(def(Propagation.REQUIRES_NEW));

        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isNotNull();
        manager.commit(inner);
        manager.commit(outer);
    }

    @Test
    void begin_supports_noExisting_noConnection() throws SQLException {
        JdbcTransaction status = beginTx(def(Propagation.SUPPORTS));
        assertThat(status.getConnection()).isNull();
        manager.commit(status);
    }

    @Test
    void begin_supports_existing_joins() throws SQLException {
        stubConnection();
        JdbcTransaction outer = beginTx(def(Propagation.REQUIRED));
        JdbcTransaction status = beginTx(def(Propagation.SUPPORTS));
        assertThat(status.getConnection()).isSameAs(outer.getConnection());
        manager.commit(status);
        manager.commit(outer);
    }

    @Test
    void begin_notSupported_suspendsExisting() throws SQLException {
        stubConnection();
        JdbcTransaction outer = beginTx(def(Propagation.REQUIRED));
        JdbcTransaction status = beginTx(def(Propagation.NOT_SUPPORTED));

        assertThat(status.getConnection()).isNull();
        manager.commit(status);
        manager.commit(outer);
    }

    @Test
    void begin_notSupported_noExisting_noConnection() {
        JdbcTransaction status = beginTx(def(Propagation.NOT_SUPPORTED));
        assertThat(status.getConnection()).isNull();
        manager.commit(status);
    }

    @Test
    void begin_never_existing_throws() throws SQLException {
        stubConnection();
        beginTx(def(Propagation.REQUIRED));
        assertThatThrownBy(() -> beginTx(def(Propagation.NEVER)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void begin_never_noExisting_noConnection() {
        // NEVER 且无既有事务：返回无连接的空事务，不抛异常
        JdbcTransaction status = beginTx(def(Propagation.NEVER));
        assertThat(status.getConnection()).isNull();
        manager.commit(status);
    }

    @Test
    void begin_mandatory_noExisting_throws() {
        assertThatThrownBy(() -> beginTx(def(Propagation.MANDATORY)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void begin_mandatory_existing_joins() throws SQLException {
        stubConnection();
        JdbcTransaction outer = beginTx(def(Propagation.REQUIRED));
        JdbcTransaction status = beginTx(def(Propagation.MANDATORY));
        assertThat(status.getConnection()).isSameAs(outer.getConnection());
        manager.commit(status);
        manager.commit(outer);
    }

    @Test
    void begin_sqlException_wraps() throws SQLException {
        when(provider.getConnection()).thenThrow(new SQLException("db down"));
        assertThatThrownBy(() -> beginTx(def(Propagation.REQUIRED)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("开启事务失败");
    }

    @Test
    void begin_readOnly_setsReadOnly() throws SQLException {
        stubConnection();
        beginTx(new TransactionDefinition(Propagation.REQUIRED, Isolation.DEFAULT, -1, true));
        verify(real).setReadOnly(true);
    }

    @Test
    void begin_isolation_setsIsolation() throws SQLException {
        stubConnection();
        beginTx(new TransactionDefinition(Propagation.REQUIRED, Isolation.READ_COMMITTED, -1, false));
        verify(real).setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
    }

    @Test
    void begin_readOnly_unsupportedDriver_ignores() throws SQLException {
        stubConnection();
        doThrow(new SQLException("not supported")).when(real).setReadOnly(true);
        // 不抛异常，事务正常开启
        JdbcTransaction status = beginTx(new TransactionDefinition(
                Propagation.REQUIRED, Isolation.DEFAULT, -1, true));
        assertThat(status.getConnection()).isNotNull();
        manager.commit(status);
    }

    @Test
    void commit_autoCommitRestoreFails_ignored() throws SQLException {
        stubConnection();
        doThrow(new SQLException("restore failed")).when(real).setAutoCommit(true);
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));
        // 恢复 autoCommit 失败被忽略，提交本身成功
        manager.commit(status);
        verify(real).commit();
    }

    // ===================== commit / rollback =====================

    @Test
    void commit_newTransaction_commits() throws SQLException {
        stubConnection();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));
        manager.commit(status);

        verify(real).commit();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }

    @Test
    void commit_rollbackOnly_rollsBack() throws SQLException {
        stubConnection();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));
        TransactionSynchronizationManager.getConnectionHolder().setRollbackOnly();
        manager.commit(status);

        verify(real, never()).commit();
        verify(real).rollback();
    }

    @Test
    void commit_joinOnly_propagatesRollbackIntent() throws SQLException {
        stubConnection();
        TransactionStatus outer = beginTx(def(Propagation.REQUIRED));
        TransactionStatus inner = beginTx(def(Propagation.REQUIRED));
        TransactionSynchronizationManager.getConnectionHolder().setRollbackOnly();

        manager.commit(inner);
        verify(real, never()).commit();

        manager.commit(outer);
        verify(real).rollback();
    }

    @Test
    void rollback_newTransaction_rollsBack() throws SQLException {
        stubConnection();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));
        manager.rollback(status);

        verify(real).rollback();
        assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
    }

    @Test
    void rollback_joinOnly_propagatesIntent() throws SQLException {
        stubConnection();
        beginTx(def(Propagation.REQUIRED));
        TransactionStatus inner = beginTx(def(Propagation.REQUIRED));

        manager.rollback(inner);
        assertThat(TransactionSynchronizationManager.getConnectionHolder().isRollbackOnly()).isTrue();
    }

    @Test
    void commit_twice_throws() throws SQLException {
        stubConnection();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));
        manager.commit(status);
        assertThatThrownBy(() -> manager.commit(status))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法重复提交");
    }

    @Test
    void commit_connectionNull_marksComplete() {
        JdbcTransaction status = beginTx(def(Propagation.SUPPORTS));
        manager.commit(status);
        // 无连接时提交不抛
        assertThat(status.getConnection()).isNull();
    }

    @Test
    void rollback_connectionNull_marksComplete() {
        JdbcTransaction status = beginTx(def(Propagation.SUPPORTS));
        manager.rollback(status);
        // 无连接时回滚不抛
        assertThat(status.getConnection()).isNull();
    }

    @Test
    void rollback_twice_throws() throws SQLException {
        stubConnection();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));
        manager.rollback(status);
        assertThatThrownBy(() -> manager.rollback(status))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("无法重复回滚");
    }

    @Test
    void commit_sqlException_wraps() throws SQLException {
        stubConnection();
        doThrow(new SQLException("commit failed")).when(real).commit();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));

        assertThatThrownBy(() -> manager.commit(status))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("事务提交失败");
    }

    @Test
    void rollback_sqlException_wraps() throws SQLException {
        stubConnection();
        doThrow(new SQLException("rollback failed")).when(real).rollback();
        TransactionStatus status = beginTx(def(Propagation.REQUIRED));

        assertThatThrownBy(() -> manager.rollback(status))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("事务回滚失败");
    }

    @Test
    void begin_getConnectionFailure_doesNotBind() throws SQLException {
        when(provider.getConnection()).thenThrow(new SQLException("boom"));
        assertThatThrownBy(() -> beginTx(def(Propagation.REQUIRED)))
                .isInstanceOf(DataAccessException.class);
        assertThat(TransactionSynchronizationManager.getConnectionHolder()).isNull();
    }
}
