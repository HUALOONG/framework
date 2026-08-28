package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;

/**
 * JDBC 事务状态实现。
 *
 * <p>持有事务连接（可能为 {@link ConnectionProxy} 代理）、真实连接（用于最终提交/回滚/关闭）、
 * 是否新开事务、以及被挂起连接的引用。除实现 {@link TransactionStatus} 的回滚/完成标记外，
 * 额外暴露连接与挂起信息供 {@link JdbcTransactionManager} 使用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class JdbcTransaction implements TransactionStatus {

    private final @Nullable Connection connection;
    private final boolean newTransaction;
    private final @Nullable Connection realConnection;
    private final @Nullable ConnectionHolder suspendedHolder;
    private boolean rollbackOnly;
    private boolean completed;

    public JdbcTransaction(@Nullable Connection connection, boolean newTransaction,
                           @Nullable Connection realConnection, @Nullable ConnectionHolder suspendedHolder) {
        this.connection = connection;
        this.newTransaction = newTransaction;
        this.realConnection = realConnection;
        this.suspendedHolder = suspendedHolder;
        this.rollbackOnly = false;
        this.completed = false;
    }

    /**
     * 事务连接（可能代理）。无事务上下文（如 SUPPORTS 未加入既有事务）时返回 {@code null}。
     *
     * @return 连接，可能为 {@code null}
     */
    public @Nullable Connection getConnection() {
        return connection;
    }

    public boolean isNewTransaction() {
        return newTransaction;
    }

    /**
     * 真实物理连接，供管理器提交/回滚/关闭。
     *
     * @return 真实连接，可能为 {@code null}
     */
    public @Nullable Connection getRealConnection() {
        return realConnection;
    }

    /**
     * 被本事务挂起的外部事务连接持有者（REQUIRES_NEW 场景）。
     *
     * @return 被挂起的持有者，可能为 {@code null}
     */
    public @Nullable ConnectionHolder getSuspendedHolder() {
        return suspendedHolder;
    }

    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public boolean isCompleted() {
        return completed;
    }

    /** 标记事务已完成。 */
    public void setCompleted() {
        this.completed = true;
    }
}
