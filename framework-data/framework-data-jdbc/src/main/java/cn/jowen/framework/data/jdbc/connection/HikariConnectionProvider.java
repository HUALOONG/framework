package cn.jowen.framework.data.jdbc.connection;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import com.zaxxer.hikari.HikariDataSource;
import org.jspecify.annotations.NullMarked;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * HikariCP 连接提供者：包装 {@link HikariDataSource}。
 *
 * <p>依据 {@link DataSourceProperties} 设置 jdbcUrl/用户名/密码/驱动及连接池参数
 * （maximumPoolSize / connectionTimeout / idleTimeout / maxLifetime）。若未显式指定驱动，由 JDBC URL 自动识别。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class HikariConnectionProvider implements ConnectionProvider {

    private final HikariDataSource dataSource;

    public HikariConnectionProvider(DataSourceProperties properties) {
        this.dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(properties.getUrl());
        if (properties.getUsername() != null) {
            dataSource.setUsername(properties.getUsername());
        }
        if (properties.getPassword() != null) {
            dataSource.setPassword(properties.getPassword());
        }
        if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
            dataSource.setDriverClassName(properties.getDriverClassName());
        }
        if (properties.getMaximumPoolSize() > 0) {
            dataSource.setMaximumPoolSize(properties.getMaximumPoolSize());
        }
        if (properties.getConnectionTimeout() > 0) {
            dataSource.setConnectionTimeout(properties.getConnectionTimeout());
        }
        if (properties.getIdleTimeout() > 0) {
            dataSource.setIdleTimeout(properties.getIdleTimeout());
        }
        if (properties.getMaxLifetime() > 0) {
            dataSource.setMaxLifetime(properties.getMaxLifetime());
        }
        dataSource.setPoolName("framework-data-jdbc-" + properties.getName());
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (!dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean isClosed() {
        return dataSource.isClosed();
    }
}
