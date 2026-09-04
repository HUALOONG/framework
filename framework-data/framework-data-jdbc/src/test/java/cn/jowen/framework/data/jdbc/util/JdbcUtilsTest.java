package cn.jowen.framework.data.jdbc.util;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcUtilsTest {

    @Test
    void closeQuietly_connectionNull_noop() {
        JdbcUtils.closeQuietly((Connection) null);
    }

    @Test
    void closeQuietly_connection_closesAndIgnoresException() throws SQLException {
        Connection conn = mock(Connection.class);
        JdbcUtils.closeQuietly(conn);
        verify(conn).close();

        Connection bad = mock(Connection.class);
        doThrow(new SQLException("x")).when(bad).close();
        JdbcUtils.closeQuietly(bad);
    }

    @Test
    void closeQuietly_statementNull_noop() {
        JdbcUtils.closeQuietly((Statement) null);
    }

    @Test
    void closeQuietly_statement_closesAndIgnoresException() throws SQLException {
        Statement stmt = mock(Statement.class);
        JdbcUtils.closeQuietly(stmt);
        verify(stmt).close();

        Statement bad = mock(Statement.class);
        doThrow(new SQLException("x")).when(bad).close();
        JdbcUtils.closeQuietly(bad);
    }

    @Test
    void closeQuietly_resultSetNull_noop() {
        JdbcUtils.closeQuietly((ResultSet) null);
    }

    @Test
    void closeQuietly_resultSet_closesAndIgnoresException() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        JdbcUtils.closeQuietly(rs);
        verify(rs).close();

        ResultSet bad = mock(ResultSet.class);
        doThrow(new SQLException("x")).when(bad).close();
        JdbcUtils.closeQuietly(bad);
    }

    @Test
    void resultSetToMaps_returnsRows() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(2);
        when(md.getColumnLabel(1)).thenReturn("ID");
        when(md.getColumnLabel(2)).thenReturn("NAME");
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getObject(1)).thenReturn(1, 2);
        when(rs.getObject(2)).thenReturn("a", "b");

        List<Map<String, Object>> rows = JdbcUtils.resultSetToMaps(rs);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsEntry("id", 1).containsEntry("name", "a");
        assertThat(rows.get(1)).containsEntry("id", 2).containsEntry("name", "b");
    }

    @Test
    void resultSetToMaps_fallsBackToColumnNameWhenLabelEmpty() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(1);
        when(md.getColumnLabel(1)).thenReturn("");
        when(md.getColumnName(1)).thenReturn("COL1");
        when(rs.next()).thenReturn(true, false);
        when(rs.getObject(1)).thenReturn("v");

        List<Map<String, Object>> rows = JdbcUtils.resultSetToMaps(rs);
        assertThat(rows.get(0)).containsKey("col1");
    }

    @Test
    void getColumnNames_returnsLabels() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(2);
        when(md.getColumnLabel(1)).thenReturn("A");
        when(md.getColumnLabel(2)).thenReturn("B");

        assertThat(JdbcUtils.getColumnNames(rs)).containsExactly("A", "B");
    }

    @Test
    void getColumnNames_fallsBackToColumnNameWhenLabelEmpty() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData md = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(md);
        when(md.getColumnCount()).thenReturn(1);
        when(md.getColumnLabel(1)).thenReturn("");
        when(md.getColumnName(1)).thenReturn("COL1");

        assertThat(JdbcUtils.getColumnNames(rs)).containsExactly("COL1");
    }
}
