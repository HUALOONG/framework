package cn.jowen.framework.data.core.datasource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 数据源配置属性，供实现层读取后组装 {@link DataSource}。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class DataSourceProperties {
    /**
     * 数据源名称，默认 primary。
     */
    private String name;

    /**
     * 数据库连接地址，jdbc:mysql://127.0.0.1:3306/test
     */
    private String url = "";

    /**
     * 数据库用户名
     */
    private String username = "";

    /**
     * 数据库密码
     */
    private @Nullable String password;

    /**
     * 数据库驱动类名
     */
    private @Nullable String driverClassName;

    /**
     * 连接池类型
     */
    private PoolType poolType;

    /**
     * 连接池最大连接数
     */
    private int maximumPoolSize;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectionTimeout;

    /**
     * 空闲连接超时时间（毫秒）
     */
    private long idleTimeout;

    /**
     * 最大存活时间（毫秒）
     */
    private long maxLifetime;

    /**
     * 是否只读
     */
    private boolean readOnly;

    /**
     * 备用数据源
     */
    private List<DataSourceProperties> secondary;

    /**
     * 默认构造函数
     */
    public DataSourceProperties() {
        this.name = "primary";
        this.poolType = PoolType.HIKARI;
        this.maximumPoolSize = 20;
        this.connectionTimeout = 5000;
        this.idleTimeout = 600000;
        this.maxLifetime = 1800000;
        this.secondary = List.of();
    }

    /**
     * 获取数据源名称
     * @return 数据源名称
     */
    public String getName() { return name; }

    /**
     * 设置数据源名称
     * @param name 数据源名称
     */
    public void setName(String name) { this.name = name; }

    /**
     * 获取数据库连接地址
     * @return 数据库连接地址
     */
    public String getUrl() { return url; }

    /**
     * 设置数据库连接地址
     * @param url 数据库连接地址
     */
    public void setUrl(String url) { this.url = url; }

    /**
     * 获取数据库用户名
     * @return 数据库用户名
     */
    public String getUsername() { return username; }

    /**
     * 设置数据库用户名
     * @param username 数据库用户名
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * 获取数据库密码
     * @return 数据库密码
     */
    public @Nullable String getPassword() { return password; }

    /**
     * 设置数据库密码
     * @param password 数据库密码
     */
    public void setPassword(@Nullable String password) { this.password = password; }

    /**
     * 获取数据库驱动类名
     * @return 数据库驱动类名
     */
    public @Nullable String getDriverClassName() { return driverClassName; }

    /**
     * 设置数据库驱动类名
     * @param driverClassName 数据库驱动类名
     */
    public void setDriverClassName(@Nullable String driverClassName) { this.driverClassName = driverClassName; }

    /**
     * 获取连接池类型
     * @return 连接池类型
     */
    public PoolType getPoolType() { return poolType; }

    /**
     * 设置连接池类型
     * @param poolType 连接池类型
     */
    public void setPoolType(PoolType poolType) { this.poolType = poolType; }

    /**
     * 获取连接池最大连接数
     * @return 连接池最大连接数
     */
    public int getMaximumPoolSize() { return maximumPoolSize; }

    /**
     * 设置连接池最大连接数
     * @param maximumPoolSize 连接池最大连接数
     */
    public void setMaximumPoolSize(int maximumPoolSize) { this.maximumPoolSize = maximumPoolSize; }

    /**
     * 获取连接超时时间
     * @return 连接超时时间
     */
    public long getConnectionTimeout() { return connectionTimeout; }

    /**
     * 设置连接超时时间
     * @param connectionTimeout 连接超时时间
     */
    public void setConnectionTimeout(long connectionTimeout) { this.connectionTimeout = connectionTimeout; }

    /**
     * 获取空闲连接超时时间
     * @return 空闲连接超时时间
     */
    public long getIdleTimeout() { return idleTimeout; }

    /**
     * 设置空闲连接超时时间
     * @param idleTimeout 空闲连接超时时间
     */
    public void setIdleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; }

    /**
     * 获取最大存活时间
     * @return 最大存活时间
     */
    public long getMaxLifetime() { return maxLifetime; }

    /**
     * 设置最大存活时间
     * @param maxLifetime 最大存活时间
     */
    public void setMaxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; }

    /**
     * 是否只读
     * @return 是否只读
     */
    public boolean isReadOnly() { return readOnly; }

    /**
     * 设置是否只读
     * @param readOnly 是否只读
     */
    public void setReadOnly(boolean readOnly) { this.readOnly = readOnly; }

    /**
     * 获取备用数据源
     * @return 备用数据源
     */
    public List<DataSourceProperties> getSecondary() { return secondary; }

    /**
     * 设置备用数据源
     * @param secondary 备用数据源
     */
    public void setSecondary(List<DataSourceProperties> secondary) { this.secondary = secondary; }
}
