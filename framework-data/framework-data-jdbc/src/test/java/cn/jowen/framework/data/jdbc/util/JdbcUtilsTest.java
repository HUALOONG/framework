package cn.jowen.framework.data.jdbc.util;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link JdbcUtils} 单元测试。
 */
class JdbcUtilsTest {

    @Test
    void closeQuietly_nullStatement_returns() {
        JdbcUtils.closeQuietly((Statement) null);
    }

    @Test
    void closeQuietly_nullResultSet_returns() {
        JdbcUtils.closeQuietly((ResultSet) null);
    }

    @Test
    void closeQuietly_statementThrowing_ignored() throws SQLException {
        Statement statement = mock(Statement.class);
        doThrow(new SQLException("close failed")).when(statement).close();

        // 关闭异常被静默忽略
        JdbcUtils.closeQuietly(statement);

        verify(statement).close();
    }

    @Test
    void closeQuietly_resultSetThrowing_ignored() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        doThrow(new SQLException("close failed")).when(resultSet).close();

        JdbcUtils.closeQuietly(resultSet);

        verify(resultSet).close();
    }

    @Test
    void closeQuietly_connectionNull_returns() {
        JdbcUtils.closeQuietly((Connection) null);
    }

    @Test
    void closeQuietly_connectionThrowing_ignored() throws SQLException {
        Connection connection = mock(Connection.class);
        doThrow(new SQLException("close failed")).when(connection).close();

        JdbcUtils.closeQuietly(connection);

        verify(connection).close();
    }
}
