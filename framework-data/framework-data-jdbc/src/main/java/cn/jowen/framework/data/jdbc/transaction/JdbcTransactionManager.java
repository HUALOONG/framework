package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.core.exception.DataAccessException;
import cn.jowen.framework.data.core.transaction.Isolation;
import cn.jowen.framework.data.core.transaction.TransactionDefinition;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.core.transaction.TransactionStatus;
import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.ConnectionProxy;
import cn.jowen.framework.data.jdbc.util.JdbcUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * JDBC 事务管理器：实现 core 的 {@link TransactionManager}。
 *
 * <p>通过 {@link ConnectionProvider} 获取连接并借 {@link TransactionSynchronizationManager} 绑定到当前线程，
 * 使同一事务内的 SQL 复用同一连接。传播行为支持：
 * <ul>
 *   <li>{@code REQUIRED}：已存在事务则加入（不重复提交/回滚）；否则新开；</li>
 *   <li>{@code REQUIRES_NEW}：挂起当前事务，新开独立事务，完成后恢复；</li>
 *   <li>{@code SUPPORTS}/{@code NOT_SUPPORTED}/{@code NEVER}/{@code MANDATORY}：按语义简化处理；</li>
 *   <li>{@code NESTED}：当前简化为 {@code REQUIRED}（未实现 JDBC 保存点，已在注释说明）。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class JdbcTransactionManager implements TransactionManager {

    private final ConnectionProvider connectionProvider;

    public JdbcTransactionManager(ConnectionProvider connectionProvider) {
        this.connectionProvider = connectionProvider;
    }

    @Override
    public TransactionStatus begin(TransactionDefinition definition) {
        ConnectionHolder existing = TransactionSynchronizationManager.getConnectionHolder();
        return switch (definition.getPropagation()) {
            case REQUIRED, NESTED -> beginRequired(existing, definition);
            case REQUIRES_NEW -> beginRequiresNew(existing, definition);
            case SUPPORTS -> beginSupports(existing, definition);
            case NOT_SUPPORTED -> beginNotSupported(existing, definition);
            case NEVER -> beginNever(existing, definition);
            case MANDATORY -> beginMandatory(existing, definition);
        };
    }

    private TransactionStatus beginRequired(@Nullable ConnectionHolder existing, TransactionDefinition definition) {
        if (existing != null) {
            // 加入既有事务，不重复开启
            return new JdbcTransaction(existing.getConnection(), false, null, null);
        }
        return startNewTransaction(definition, null);
    }

    private TransactionStatus beginRequiresNew(@Nullable ConnectionHolder existing, TransactionDefinition definition) {
        if (existing != null) {
            TransactionSynchronizationManager.unbindResource();
        }
        return startNewTransaction(definition, existing);
    }

    private TransactionStatus beginSupports(@Nullable ConnectionHolder existing, TransactionDefinition definition) {
        if (existing != null) {
            return new JdbcTransaction(existing.getConnection(), false, null, null);
        }
        return new JdbcTransaction(null, false, null, null);
    }

    private TransactionStatus beginNotSupported(@Nullable ConnectionHolder existing, TransactionDefinition definition) {
        ConnectionHolder suspended = existing;
        if (existing != null) {
            TransactionSynchronizationManager.unbindResource();
        }
        return new JdbcTransaction(null, false, null, suspended);
    }

    private TransactionStatus beginNever(@Nullable ConnectionHolder existing, TransactionDefinition definition) {
        if (existing != null) {
            throw new IllegalStateException("当前已存在事务，NEVER 传播行为不允许");
        }
        return new JdbcTransaction(null, false, null, null);
    }

    private TransactionStatus beginMandatory(@Nullable ConnectionHolder existing, TransactionDefinition definition) {
        if (existing == null) {
            throw new IllegalStateException("当前不存在事务，MANDATORY 传播行为要求已存在事务");
        }
        return new JdbcTransaction(existing.getConnection(), false, null, null);
    }

    private TransactionStatus startNewTransaction(TransactionDefinition definition, @Nullable ConnectionHolder suspended) {
        try {
            Connection real = connectionProvider.getConnection();
            real.setAutoCommit(false);
            if (definition.getIsolation() != Isolation.DEFAULT) {
                real.setTransactionIsolation(IsolationLevelManager.toJdbc(definition.getIsolation()));
            }
            if (definition.isReadOnly()) {
                try {
                    real.setReadOnly(true);
                } catch (SQLException ignored) {
                    // 部分驱动不支持只读，忽略
                }
            }
            Connection proxy = ConnectionProxy.wrap(real, c -> { /* 事务内 close 由管理器统一控制 */ });
            ConnectionHolder holder = new ConnectionHolder(proxy, true);
            TransactionSynchronizationManager.bindResource(holder);
            return new JdbcTransaction(proxy, true, real, suspended);
        } catch (SQLException e) {
            throw new DataAccessException("开启事务失败", e);
        }
    }

    @Override
    public void commit(TransactionStatus status) {
        JdbcTransaction tx = (JdbcTransaction) status;
        if (tx.isCompleted()) {
            throw new IllegalStateException("事务已完成，无法重复提交");
        }
        if (tx.getConnection() == null) {
            tx.setCompleted();
            return;
        }
        if (!tx.isNewTransaction()) {
            // 加入既有事务：仅传播回滚意图，提交/回滚由外层负责
            if (tx.isRollbackOnly()) {
                ConnectionHolder holder = TransactionSynchronizationManager.getConnectionHolder();
                if (holder != null) {
                    holder.setRollbackOnly();
                }
            }
            tx.setCompleted();
            return;
        }
        Connection real = tx.getRealConnection();
        boolean rollbackOnly = tx.isRollbackOnly()
                || Boolean.TRUE.equals(holderRollbackOnly());
        try {
            TransactionSynchronizationManager.triggerBeforeCommit();
            if (rollbackOnly) {
                real.rollback();
            } else {
                real.commit();
            }
            TransactionSynchronizationManager.triggerAfterCommit();
        } catch (SQLException e) {
            throw new DataAccessException("事务提交失败", e);
        } finally {
            finishTransaction(tx, real);
        }
    }

    @Override
    public void rollback(TransactionStatus status) {
        JdbcTransaction tx = (JdbcTransaction) status;
        if (tx.isCompleted()) {
            throw new IllegalStateException("事务已完成，无法重复回滚");
        }
        if (tx.getConnection() == null) {
            tx.setCompleted();
            return;
        }
        if (!tx.isNewTransaction()) {
            // 加入既有事务：将回滚意图传播给外层
            ConnectionHolder holder = TransactionSynchronizationManager.getConnectionHolder();
            if (holder != null) {
                holder.setRollbackOnly();
            }
            tx.setCompleted();
            return;
        }
        Connection real = tx.getRealConnection();
        try {
            real.rollback();
        } catch (SQLException e) {
            throw new DataAccessException("事务回滚失败", e);
        } finally {
            finishTransaction(tx, real);
        }
    }

    private @Nullable Boolean holderRollbackOnly() {
        ConnectionHolder holder = TransactionSynchronizationManager.getConnectionHolder();
        return holder != null ? holder.isRollbackOnly() : null;
    }

    private void finishTransaction(JdbcTransaction tx, @Nullable Connection real) {
        try {
            if (real != null) {
                real.setAutoCommit(true);
                JdbcUtils.closeQuietly(real);
            }
        } catch (SQLException ignored) {
            // 忽略恢复异常
        } finally {
            ConnectionHolder suspended = tx.getSuspendedHolder();
            TransactionSynchronizationManager.unbindResource();
            if (suspended != null) {
                TransactionSynchronizationManager.bindResource(suspended);
            }
            TransactionSynchronizationManager.triggerAfterCompletion();
            tx.setCompleted();
        }
    }
}
