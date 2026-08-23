package cn.jowen.framework.data.jdbc.core;

import cn.jowen.framework.data.jdbc.mapping.MapRowMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 原生 SQL 执行器：对 {@link JdbcTemplate} 的轻量封装，便于无 {@link cn.jowen.framework.data.core.mapping.RowMapper} 的查询，
 * 直接返回「列名 → 值」的 {@code Map}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SqlRunner {

    private final JdbcOperations jdbcOperations;

    public SqlRunner(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    /**
     * 执行查询，返回多行 Map。
     *
     * @param sql  SQL，不可为 {@code null}
     * @param args 参数
     * @return 行 Map 列表，不可为 {@code null}
     */
    public List<Map<String, Object>> queryForList(String sql, Object... args) {
        return jdbcOperations.query(sql, MapRowMapper.INSTANCE, args);
    }

    /**
     * 执行查询，返回单行 Map；无结果返回 {@code null}。
     *
     * @param sql  SQL，不可为 {@code null}
     * @param args 参数
     * @return 单行 Map，可能为 {@code null}
     */
    public @Nullable Map<String, Object> queryOne(String sql, Object... args) {
        List<Map<String, Object>> list = jdbcOperations.query(sql, MapRowMapper.INSTANCE, args);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 执行更新/删除。
     *
     * @param sql  SQL，不可为 {@code null}
     * @param args 参数
     * @return 受影响行数
     */
    public int update(String sql, Object... args) {
        return jdbcOperations.update(sql, args);
    }

    /**
     * 执行任意 SQL（如 DDL）。
     *
     * @param sql SQL，不可为 {@code null}
     */
    public void execute(String sql) {
        jdbcOperations.execute(sql);
    }
}
