package cn.jowen.framework.data.core.callback;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.Statement;

/**
 * Statement 回调，允许在执行数据库操作前/后拦截 {@link Connection} 和 {@link Statement}。
 *
 * @param <T> 返回值类型
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface StatementCallback<T> {

    /**
     * 在 Statement 上执行操作。
     *
     * @param connection 数据库连接，不可为 {@code null}
     * @param statement  已创建的 Statement，不可为 {@code null}
     * @return 操作结果，可为 {@code null}
     * @throws Exception 操作期间抛出的异常
     */
    @Nullable T doInStatement(Connection connection, Statement statement) throws Exception;
}
