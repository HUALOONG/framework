package cn.jowen.framework.data.jdbc.core;

import cn.jowen.framework.data.core.mapping.RowMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * JDBC 操作接口：封装查询、更新、执行与批量等操作。
 *
 * <p>约定：结果集先由模板读取为「列名 → 值」的 {@code Map} 列表，再交给 {@link RowMapper} 完成对象映射，
 * 与 core 抽象层解耦。具体实现见 {@link JdbcTemplate}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface JdbcOperations {

    /**
     * 查询并映射为对象列表。
     *
     * @param sql       SQL，不可为 {@code null}
     * @param rowMapper 行映射器，不可为 {@code null}
     * @param args      参数，按位置绑定
     * @param <T>       元素类型
     * @return 对象列表，不可为 {@code null}
     */
    <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args);

    /**
     * 查询单行并映射为对象；无结果抛异常。
     *
     * @param sql       SQL，不可为 {@code null}
     * @param rowMapper 行映射器，不可为 {@code null}
     * @param args      参数
     * @param <T>       元素类型
     * @return 单个对象，不可为 {@code null}
     */
    <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args);

    /**
     * 查询单行首列并转换为指定类型。
     *
     * @param sql          SQL，不可为 {@code null}
     * @param requiredType 目标类型，不可为 {@code null}
     * @param args         参数
     * @param <T>          元素类型
     * @return 标量值，不可为 {@code null}
     */
    <T> T queryForObject(String sql, Class<T> requiredType, Object... args);

    /**
     * 查询多行首列并转换为指定类型列表。
     *
     * @param sql         SQL，不可为 {@code null}
     * @param elementType 元素类型，不可为 {@code null}
     * @param args        参数
     * @param <T>         元素类型
     * @return 标量列表，不可为 {@code null}
     */
    <T> List<T> queryForList(String sql, Class<T> elementType, Object... args);

    /**
     * 执行更新/删除，返回受影响行数。
     *
     * @param sql  SQL，不可为 {@code null}
     * @param args 参数
     * @return 受影响行数
     */
    int update(String sql, Object... args);

    /**
     * 执行任意 SQL（DDL/DML），不返回结果。
     *
     * @param sql SQL，不可为 {@code null}
     */
    void execute(String sql);

    /**
     * 批量执行多条不同 SQL。
     *
     * @param sql 多条 SQL，不可为 {@code null}
     * @return 每条语句受影响行数
     */
    int[] batchUpdate(String... sql);

    /**
     * 批量执行同构 SQL（参数化）。
     *
     * @param sql        SQL 模板，不可为 {@code null}
     * @param batchArgs 多组参数
     * @return 每组受影响行数
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs);

    /**
     * 查询并返回「列名 → 值」的原始 Map 列表（无 RowMapper）。
     *
     * @param sql  SQL，不可为 {@code null}
     * @param args 参数
     * @return 行 Map 列表，不可为 {@code null}
     */
    List<Map<String, Object>> queryForMaps(String sql, Object... args);

    /**
     * 执行插入并返回数据库生成的主键（若有）。
     *
     * @param sql  SQL，不可为 {@code null}
     * @param args 参数
     * @return 生成的主键；无则返回 {@code null}
     */
    @Nullable
    Object executeInsert(String sql, Object... args);
}
