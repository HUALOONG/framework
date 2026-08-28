package cn.jowen.framework.data.core.datasource;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 数据源元信息默认实现，仅承载连接元信息，不持有物理连接。
 *
 * @param name     数据源名称，不可为空
 * @param url      连接地址，不可为空
 * @param username 用户名，不可为空
 * @param password 密码，可为 {@code null}
 * @param poolType 连接池类型，不可为 {@code null}
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public record SimpleDataSource(
        String name,
        String url,
        String username,
        @Nullable String password,
        PoolType poolType
) implements DataSource {

    /**
     * 从配置属性构建数据源元信息。
     *
     * @param properties 数据源配置属性，不可为 {@code null}
     * @return 数据源元信息，不可为 {@code null}
     */
    public static SimpleDataSource from(DataSourceProperties properties) {
        if (properties == null) {
            throw new IllegalArgumentException("properties cannot be null");
        }
        return new SimpleDataSource(properties.getName(), properties.getUrl(), properties.getUsername(),
                properties.getPassword(), properties.getPoolType());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public PoolType getPoolType() {
        return poolType;
    }
}