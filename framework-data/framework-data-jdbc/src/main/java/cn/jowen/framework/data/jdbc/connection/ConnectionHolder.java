package cn.jowen.framework.data.jdbc.connection;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;

/**
 * 连接持有者：在事务同步管理器中保存当前绑定连接及其状态。
 *
 * <p>承载：
 * <ul>
 *   <li>当前连接（可能为 {@link ConnectionProxy} 代理，拦截 {@code close()}）；</li>
 *   <li>是否由事务绑定（决定 JdbcTemplate 用完后是否真正关闭）；</li>
 *   <li>引用计数（供未来多层嵌套资源复用）；</li>
 *   <li>回滚仅标记（供 REQUIRED 加入方传播回滚意图）。</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class ConnectionHolder {

    private final Connection connection;
    private final boolean transactionBound;
    private int referenceCount;
    private boolean rollbackOnly;

    public ConnectionHolder(Connection connection, boolean transactionBound) {
        this.connection = connection;
        this.transactionBound = transactionBound;
        this.referenceCount = 1;
        this.rollbackOnly = false;
    }

    public Connection getConnection() {
        return connection;
    }

    public boolean isTransactionBound() {
        return transactionBound;
    }

    public int getReferenceCount() {
        return referenceCount;
    }

    public void increment() {
        referenceCount++;
    }

    public int decrement() {
        referenceCount = Math.max(0, referenceCount - 1);
        return referenceCount;
    }

    public boolean isRollbackOnly() {
        return rollbackOnly;
    }

    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }

    @Override
    public @Nullable String toString() {
        return "ConnectionHolder{bound=" + transactionBound + ", ref=" + referenceCount
                + ", rollbackOnly=" + rollbackOnly + "}";
    }
}
