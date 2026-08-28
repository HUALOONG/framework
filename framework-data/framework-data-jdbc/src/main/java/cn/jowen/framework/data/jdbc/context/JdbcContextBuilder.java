package cn.jowen.framework.data.jdbc.context;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.interceptor.SqlInterceptor;
import org.jspecify.annotations.NullMarked;

/**
 * 轻量流式构建器：以最简 API 装配 {@link JdbcContext}（零 Spring 依赖）。
 *
 * <p>作为 {@link JdbcContext.Builder} 的便捷门面，对外暴露极简的
 * {@code JdbcContextBuilder.create().url(...).username(...).password(...).poolType(...).build()}
 * 链式调用，便于模块在脱离任何 DI 容器的环境下独立运行与测试。
 *
 * <p>内部持有 core 的 {@link DataSourceProperties}（承载 url/username/password/driverClassName）
 * 与 {@link JdbcContext.Builder}（承载连接池类型、方言注册中心、拦截器、多租户开关），
 * {@link #build()} 时将两个配置组装并产出 {@link JdbcContext}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class JdbcContextBuilder {

    /** 数据源连接属性（url/username/password/driverClassName）。 */
    private final DataSourceProperties dataSourceProperties = new DataSourceProperties();

    /** 内部上下文构建器（连接池类型、方言、拦截器、租户开关）。 */
    private final JdbcContext.Builder inner = JdbcContext.builder();

    private JdbcContextBuilder() {
    }

    /**
     * 创建构建器实例。
     *
     * @return 构建器，不可为 {@code null}
     */
    public static JdbcContextBuilder create() {
        return new JdbcContextBuilder();
    }

    /**
     * 设置 JDBC 连接地址。
     *
     * @param url 连接地址，不可为 {@code null}
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder url(String url) {
        dataSourceProperties.setUrl(url);
        return this;
    }

    /**
     * 设置数据库用户名。
     *
     * @param username 用户名，不可为 {@code null}
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder username(String username) {
        dataSourceProperties.setUsername(username);
        return this;
    }

    /**
     * 设置数据库密码。
     *
     * @param password 密码，可为空串
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder password(String password) {
        dataSourceProperties.setPassword(password);
        return this;
    }

    /**
     * 设置数据库驱动类名（可选；留空时由 JDBC 自动发现）。
     *
     * @param driverClassName 驱动类名，可为 {@code null}
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder driverClassName(String driverClassName) {
        dataSourceProperties.setDriverClassName(driverClassName);
        return this;
    }

    /**
     * 设置连接池类型。
     *
     * @param poolType 连接池类型，不可为 {@code null}
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder poolType(PoolType poolType) {
        inner.poolType(poolType);
        return this;
    }

    /**
     * 按数据库类型设置默认方言（注册到全新的方言注册中心并设为默认）。
     *
     * @param type 数据库类型，可为 {@code null}；{@code null} 或 {@link DatabaseType#UNKNOWN} 时回退内置默认（H2）
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder dialect(DatabaseType type) {
        DialectRegistry registry = new DialectRegistry();
        if (type != null && type != DatabaseType.UNKNOWN) {
            registry.setDefault(type);
        }
        inner.dialectRegistry(registry);
        return this;
    }

    /**
     * 直接设置默认方言实例（注册到全新的方言注册中心并设为默认）。
     *
     * @param dialect 方言实例，不可为 {@code null}
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder dialect(DatabaseDialect dialect) {
        DialectRegistry registry = new DialectRegistry();
        registry.register(dialect);
        registry.setDefault(dialect);
        inner.dialectRegistry(registry);
        return this;
    }

    /**
     * 追加自定义 SQL 拦截器。
     *
     * @param interceptor 拦截器，不可为 {@code null}
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder addInterceptor(SqlInterceptor interceptor) {
        inner.addInterceptor(interceptor);
        return this;
    }

    /**
     * 是否开启多租户（开启后追加租户拦截器）。
     *
     * @param enabled 是否开启
     * @return 构建器自身，便于链式调用
     */
    public JdbcContextBuilder tenantEnabled(boolean enabled) {
        inner.enableTenant(enabled);
        return this;
    }

    /**
     * 组装并构建 {@link JdbcContext}。
     *
     * @return 上下文实例，不可为 {@code null}
     */
    public JdbcContext build() {
        inner.properties(dataSourceProperties);
        return inner.build();
    }
}
