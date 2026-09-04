package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import org.jspecify.annotations.NullMarked;

import java.sql.Connection;
import java.sql.Savepoint;
import java.util.function.Supplier;

/**
 * 嵌套事务（NESTED 传播）：基于 JDBC {@link Connection#setSavepoint() 保存点} 实现。
 *
 * <p>语义对齐 Spring {@code Propagation.NESTED}：在外层已存在事务的连接上建立保存点，
 * 内层回滚仅回退到该保存点（不影响外层事务），外层回滚则连带内层一并回退。
 * 与当前 data-jdbc 轻量事务模型一致——本类不重新引入 {@code Propagation} 枚举与完整事务管理器，
 * 仅提供保存点这一最小可用原语，供上层按需组合。
 *
 * <p>典型用法：
 * <pre>{@code
 * try (NestedTransaction tx = new NestedTransaction(holder)) {
 *     // 内层操作
 *     tx.commit();   // 释放保存点（对外层提交无影响）
 * }                  // 或 tx.rollback() 仅回退到保存点
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class NestedTransaction implements AutoCloseable {

    private final Connection connection;
    private final Savepoint savepoint;
    private boolean completed = false;

    public NestedTransaction(Connection connection) throws java.sql.SQLException {
        this.connection = connection;
        this.savepoint = connection.setSavepoint();
    }

    public NestedTransaction(ConnectionHolder holder) throws java.sql.SQLException {
        this(holder.getConnection());
    }

    /** 释放保存点（内层正常结束；对外层事务提交无副作用）。 */
    public void commit() throws java.sql.SQLException {
        if (!completed) {
            connection.releaseSavepoint(savepoint);
            completed = true;
        }
    }

    /** 回滚到保存点（仅撤销本嵌套事务内的改动）。 */
    public void rollback() throws java.sql.SQLException {
        if (!completed) {
            connection.rollback(savepoint);
            completed = true;
        }
    }

    /** 正常结束释放保存点；若已回滚/提交则幂等无操作。 */
    @Override
    public void close() throws java.sql.SQLException {
        if (!completed) {
            commit();
        }
    }

    /**
     * 在保存点内执行动作：成功则释放保存点，异常则回滚到保存点后原样抛出。
     *
     * @param connection 当前事务连接
     * @param action     内层动作
     * @param <T>        返回值类型
     * @return 动作返回值
     */
    public static <T> T execute(Connection connection, Supplier<T> action) throws java.sql.SQLException {
        try (NestedTransaction tx = new NestedTransaction(connection)) {
            try {
                T result = action.get();
                tx.commit();
                return result;
            } catch (RuntimeException ex) {
                tx.rollback();
                throw ex;
            }
        }
    }
}
