package cn.jowen.framework.data.jdbc.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SqlRunner} 测试。
 */
class SqlRunnerTest {

    private final JdbcOperations ops = mock(JdbcOperations.class);
    private final SqlRunner runner = new SqlRunner(ops);

    @Test
    void queryForList_delegatesWithMapRowMapper() {
        Map<String, Object> row = Map.of("name", "tom");
        when(ops.query(eq("SELECT 1"), any(), any(Object[].class))).thenReturn(List.of(row));

        List<Map<String, Object>> result = runner.queryForList("SELECT 1", "arg");

        assertThat(result).containsExactly(row);
    }

    @Test
    void queryOne_empty_returnsNull() {
        when(ops.query(eq("SELECT 1"), any(), any(Object[].class))).thenReturn(List.of());
        assertThat(runner.queryOne("SELECT 1")).isNull();
    }

    @Test
    void queryOne_firstRow() {
        Map<String, Object> row = Map.of("name", "tom");
        when(ops.query(eq("SELECT 1"), any(), any(Object[].class))).thenReturn(List.of(row));
        assertThat(runner.queryOne("SELECT 1")).isEqualTo(row);
    }

    @Test
    void update_delegates() {
        when(ops.update(eq("UPDATE t SET a = 1"), any(Object[].class))).thenReturn(3);
        assertThat(runner.update("UPDATE t SET a = 1")).isEqualTo(3);
    }

    @Test
    void execute_delegates() {
        runner.execute("CREATE TABLE x (id INT)");
        verify(ops).execute("CREATE TABLE x (id INT)");
    }
}
