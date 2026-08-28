package cn.jowen.framework.data.jdbc.connection;

import java.sql.Connection;
import java.sql.SQLException;

import org.jspecify.annotations.NullMarked;

/**
 * 连接提供者：统一连接获取与释放，屏蔽底层连接池差异。
 *
 * <p>实现层提供直连（{@code SimpleConnectionProvider}）/ HikariCP / Druid 等适配。
 * 事务内复用连接由 {@link TransactionSynchronizationManager} 负责，本接口仅负责物理连接的获取与释放。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface ConnectionProvider {

    /**
     * 获取一个数据库连接。
     *
     * @return 连接，不可为 {@code null}
     * @throws SQLException 获取失败
     */
    Connection getConnection() throws SQLException;

    /**
     * 释放底层资源（如关闭连接池）。重复调用安全。
     */
    void close();

    /**
     * 是否已关闭。
     *
     * @return 已关闭返回 {@code true}
     */
    boolean isClosed();
}
