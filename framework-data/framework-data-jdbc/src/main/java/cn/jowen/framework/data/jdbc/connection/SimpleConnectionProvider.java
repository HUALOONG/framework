package cn.jowen.framework.data.jdbc.connection;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import org.jspecify.annotations.NullMarked;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * 简易连接提供者：基于 {@link DriverManager} 直连，无连接池。
 *
 * <p>作为默认实现，使模块可「零外部连接池」独立运行（测试与主路径皆可用）。
 * 通过 core 的 {@link DataSourceProperties} 读取 url/username/password/driverClassName。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class SimpleConnectionProvider implements ConnectionProvider {

    private final DataSourceProperties properties;
    private final Set<Connection> opened = new HashSet<>();
    private volatile boolean closed = false;

    public SimpleConnectionProvider(DataSourceProperties properties) {
        this.properties = properties;
        loadDriverIfNeeded();
    }

    private void loadDriverIfNeeded() {
        String driver = properties.getDriverClassName();
        if (driver != null && !driver.isBlank()) {
            try {
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("无法加载数据库驱动：" + driver, e);
            }
        }
    }

    @Override
    public synchronized Connection getConnection() throws SQLException {
        if (closed) {
            throw new SQLException("SimpleConnectionProvider 已关闭");
        }
        Properties info = new Properties();
        if (properties.getUsername() != null) {
            info.setProperty("user", properties.getUsername());
        }
        if (properties.getPassword() != null) {
            info.setProperty("password", properties.getPassword());
        }
        Connection connection = DriverManager.getConnection(properties.getUrl(), info);
        opened.add(connection);
        return connection;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (Connection connection : new HashSet<>(opened)) {
            try {
                connection.close();
            } catch (SQLException ignored) {
                // 静默忽略
            }
        }
        opened.clear();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
