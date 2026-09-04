package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NestedTransactionTest {

    @Mock Connection connection;
    @Mock Savepoint savepoint;

    @Test
    void opensSavepointOnConstruction() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        try (NestedTransaction tx = new NestedTransaction(connection)) {
            verify(connection).setSavepoint();
        }
    }

    @Test
    void releasesSavepointOnClose() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        try (NestedTransaction tx = new NestedTransaction(connection)) {
            tx.commit();
        }
        verify(connection).releaseSavepoint(savepoint);
    }

    @Test
    void rollbackRollsBackToSavepointOnly() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        try (NestedTransaction tx = new NestedTransaction(connection)) {
            tx.rollback();
        }
        verify(connection).rollback(savepoint);
    }

    @Test
    void closingAfterRollbackDoesNotReleaseAgain() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        try (NestedTransaction tx = new NestedTransaction(connection)) {
            tx.rollback();
        }
        verify(connection).rollback(savepoint);
        // releaseSavepoint must NOT be called again after rollback
        verify(connection, org.mockito.Mockito.never()).releaseSavepoint(savepoint);
    }

    @Test
    void executeRunsActionAndReleasesOnSuccess() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        String result = NestedTransaction.execute(connection, () -> "ok");
        assertEquals("ok", result);
        verify(connection).releaseSavepoint(savepoint);
    }

    @Test
    void executeRollsBackToSavepointOnFailure() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        SQLException ex = new SQLException("boom");
        doThrow(ex).when(connection).rollback(savepoint);
        assertThrows(SQLException.class,
                () -> NestedTransaction.execute(connection, () -> {
                    throw new IllegalStateException("inner");
                }));
        verify(connection).rollback(savepoint);
    }

    @Test
    void buildsFromConnectionHolder() throws SQLException {
        when(connection.setSavepoint()).thenReturn(savepoint);
        ConnectionHolder holder = new ConnectionHolder(connection, true);
        try (NestedTransaction tx = new NestedTransaction(holder)) {
            tx.commit();
        }
        verify(connection).releaseSavepoint(savepoint);
    }
}
