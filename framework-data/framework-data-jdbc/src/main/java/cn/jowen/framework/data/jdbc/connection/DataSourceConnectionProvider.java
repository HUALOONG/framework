package cn.jowen.framework.data.jdbc.connection;

import org.jspecify.annotations.NullMarked;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 基于 {@link DataSource} 的连接提供者：从外部连接池（如 HikariCP/Druid/应用容器注入的 DataSource）获取连接。
 *
 * <p>与 {@link SimpleConnectionProvider}（DriverManager 直连）不同，本实现不负责连接池的生命周期，
 * 仅透传 {@link DataSource#getConnection()}，由调用方管理底层连接池；{@link #close()} 为空操作。
 *
 * <p>零 Spring 依赖，可在任意环境（含 Spring 容器注入的 DataSource）下使用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DataSourceConnectionProvider implements ConnectionProvider {

    private final DataSource dataSource;

    public DataSourceConnectionProvider(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        // DataSource 生命周期由外部管理，这里不关闭
    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
