package cn.jowen.framework.data.jdbc.core;

import cn.jowen.framework.data.jdbc.statement.SqlResult;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量模板：收集多条同构/异构 SQL 与其参数，统一执行批次。
 *
 * <p>{@link #addBatch(String, Object...)} 收集 SQL 与参数；{@link #executeBatch()} 按 SQL 文本分组，
 * 每组调用 {@link JdbcOperations#batchUpdate(String, List)} 执行，最终将所有分组受影响行数展平为 {@code int[]} 返回。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class BatchTemplate {

    private final JdbcOperations jdbcOperations;
    private final List<SqlResult> batch = new ArrayList<>();

    public BatchTemplate(JdbcOperations jdbcOperations) {
        this.jdbcOperations = jdbcOperations;
    }

    /**
     * 收集一条待执行的 SQL 与参数。
     *
     * @param sql    SQL，不可为 {@code null}
     * @param params 参数（按位置）
     * @return 当前模板，便于链式调用
     */
    public BatchTemplate addBatch(String sql, Object... params) {
        List<Object> list = new ArrayList<>(params.length);
        for (Object p : params) {
            list.add(p);
        }
        batch.add(new SqlResult(sql, list));
        return this;
    }

    /**
     * 收集一条待执行的 SQL 与参数列表。
     *
     * @param sql    SQL，不可为 {@code null}
     * @param params 参数列表，不可为 {@code null}
     * @return 当前模板
     */
    public BatchTemplate addBatch(String sql, List<Object> params) {
        batch.add(new SqlResult(sql, new ArrayList<>(params)));
        return this;
    }

    /**
     * 执行全部批次。
     *
     * @return 每组受影响行数展平后的数组
     */
    public int[] executeBatch() {
        if (batch.isEmpty()) {
            return new int[0];
        }
        // 按 SQL 分组
        Map<String, List<Object[]>> groups = new LinkedHashMap<>();
        for (SqlResult result : batch) {
            groups.computeIfAbsent(result.sql(), k -> new ArrayList<>())
                    .add(result.params().toArray());
        }
        List<Integer> flat = new ArrayList<>();
        for (Map.Entry<String, List<Object[]>> entry : groups.entrySet()) {
            int[] counts = jdbcOperations.batchUpdate(entry.getKey(), entry.getValue());
            for (int c : counts) {
                flat.add(c);
            }
        }
        batch.clear();
        int[] result = new int[flat.size()];
        for (int i = 0; i < flat.size(); i++) {
            result[i] = flat.get(i);
        }
        return result;
    }

    /**
     * 清空已收集的批次。
     */
    public void clear() {
        batch.clear();
    }
}
