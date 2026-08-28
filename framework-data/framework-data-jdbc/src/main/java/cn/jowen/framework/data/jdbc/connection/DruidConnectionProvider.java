package cn.jowen.framework.data.jdbc.connection;

import cn.jowen.framework.core.util.ReflectionUtils;
import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Druid 连接提供者：通过反射构造 {@code com.alibaba.druid.pool.DruidDataSource}。
 *
 * <p>由于 Druid 在本模块为 {@code optional} 依赖，此处使用反射构造，确保「无 Druid 在 classpath」时仍可编译通过；
 * 若运行期未提供 Druid，{@link #getConnection()} 将抛出明确异常。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DruidConnectionProvider implements ConnectionProvider {

    private static final String DRUID_CLASS = "com.alibaba.druid.pool.DruidDataSource";

    private final Object dataSource;

    public DruidConnectionProvider(DataSourceProperties properties) {
        try {
            Class<?> clazz = Class.forName(DRUID_CLASS);
            this.dataSource = clazz.getDeclaredConstructor().newInstance();
            ReflectionUtils.invokeMethod(this.dataSource, "setUrl", properties.getUrl());
            ReflectionUtils.invokeMethod(this.dataSource, "setUsername", properties.getUsername());
            if (properties.getPassword() != null) {
                ReflectionUtils.invokeMethod(this.dataSource, "setPassword", properties.getPassword());
            }
            if (properties.getDriverClassName() != null && !properties.getDriverClassName().isBlank()) {
                ReflectionUtils.invokeMethod(this.dataSource, "setDriverClassName", properties.getDriverClassName());
            }
            if (properties.getMaximumPoolSize() > 0) {
                ReflectionUtils.invokeMethod(this.dataSource, "setMaxActive", properties.getMaximumPoolSize());
            }
            if (properties.getConnectionTimeout() > 0) {
                ReflectionUtils.invokeMethod(this.dataSource, "setMaxWait", properties.getConnectionTimeout());
            }
            ReflectionUtils.invokeMethod(this.dataSource, "init");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("构造 DruidDataSource 失败，请确认 druid 依赖在 classpath 中", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return (Connection) ReflectionUtils.invokeMethod(dataSource, "getConnection");
        } catch (RuntimeException e) {
            Throwable cause = e.getCause();
            if (cause instanceof SQLException se) {
                throw se;
            }
            throw new SQLException("获取 Druid 连接失败", cause);
        }
    }

    @Override
    public void close() {
        try {
            ReflectionUtils.invokeMethod(dataSource, "close");
        } catch (RuntimeException ignored) {
            // 静默忽略
        }
    }

    @Override
    public boolean isClosed() {
        try {
            Object closed = ReflectionUtils.invokeMethod(dataSource, "isClosed");
            return Boolean.TRUE.equals(closed);
        } catch (RuntimeException e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    private static @Nullable Object unused() {
        return null;
    }
}
