package cn.jowen.framework.data.jdbc.statement;

import cn.jowen.framework.data.core.mapping.TypeHandlerRegistry;
import org.jspecify.annotations.NullMarked;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * 批量参数绑定器：针对同构 SQL，将多组参数依次 {@code addBatch}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class BatchParameterBinder {

    private BatchParameterBinder() {
    }

    /**
     * 将多组参数绑定到 PreparedStatement 并加入批次。
     *
     * @param stmt     PreparedStatement（已 prepare），不可为 {@code null}
     * @param batchParams 多组参数（每组为一个 {@code Object[]}），不可为 {@code null}
     * @param registry    类型处理器注册中心，不可为 {@code null}
     * @throws SQLException 绑定失败
     */
    public static void addBatches(PreparedStatement stmt, List<Object[]> batchParams,
                                   TypeHandlerRegistry registry) throws SQLException {
        for (Object[] params : batchParams) {
            ParameterBinder.bindAll(stmt, java.util.Arrays.asList(params), registry);
            stmt.addBatch();
        }
    }

    /**
     * 将多组参数（以 {@code List<Object>} 形式）绑定到 PreparedStatement 并加入批次。
     *
     * @param stmt        PreparedStatement（已 prepare），不可为 {@code null}
     * @param batchParams 多组参数，不可为 {@code null}
     * @param registry    类型处理器注册中心，不可为 {@code null}
     * @throws SQLException 绑定失败
     */
    public static void addBatchesAsLists(PreparedStatement stmt, List<List<Object>> batchParams,
                                           TypeHandlerRegistry registry) throws SQLException {
        for (List<Object> params : batchParams) {
            ParameterBinder.bindAll(stmt, params, registry);
            stmt.addBatch();
        }
    }
}
