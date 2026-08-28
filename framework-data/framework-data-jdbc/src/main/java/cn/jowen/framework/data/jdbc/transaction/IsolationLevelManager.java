package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.core.transaction.Isolation;
import org.jspecify.annotations.NullMarked;

import java.sql.Connection;

/**
 * 隔离级别管理器：将 core 的 {@link Isolation} 映射为 JDBC {@link Connection} 隔离级别常量。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class IsolationLevelManager {

    private IsolationLevelManager() {
    }

    /**
     * 映射为 JDBC 隔离级别常量。
     *
     * @param isolation core 隔离级别，不可为 {@code null}
     * @return JDBC 隔离级别常量（{@link Connection#TRANSACTION_NONE} 等）
     */
    public static int toJdbc(Isolation isolation) {
        return switch (isolation) {
            case DEFAULT -> Connection.TRANSACTION_REPEATABLE_READ; // 占位，实际由数据库默认接管
            case READ_UNCOMMITTED -> Connection.TRANSACTION_READ_UNCOMMITTED;
            case READ_COMMITTED -> Connection.TRANSACTION_READ_COMMITTED;
            case REPEATABLE_READ -> Connection.TRANSACTION_REPEATABLE_READ;
            case SERIALIZABLE -> Connection.TRANSACTION_SERIALIZABLE;
        };
    }
}
