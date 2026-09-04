package cn.jowen.framework.data.jdbc.context;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.mapping.NamingStrategy;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.DruidConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.HikariConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.core.BatchTemplate;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.core.NamedParameterTemplate;
import cn.jowen.framework.data.jdbc.core.SqlRunner;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.exception.SQLExceptionTranslator;
import cn.jowen.framework.data.jdbc.interceptor.InterceptorChain;
import cn.jowen.framework.data.jdbc.interceptor.LoggingInterceptor;
import cn.jowen.framework.data.jdbc.interceptor.PerformanceInterceptor;
import cn.jowen.framework.data.jdbc.interceptor.SqlInterceptor;
import cn.jowen.framework.data.jdbc.interceptor.TenantInterceptor;
import cn.jowen.framework.data.jdbc.mapping.CamelCaseNamingStrategy;
import cn.jowen.framework.data.jdbc.mapping.DefaultEntityMetadataResolver;
import cn.jowen.framework.data.jdbc.mapping.DefaultTypeHandlers;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 轻量 IoC 上下文（可独立运行 + 依赖注入）。
 *
 * <p>以纯 Java 方式装配本模块全部核心组件：连接提供者、方言注册中心、类型处理器、
 * 实体元信息解析器、异常翻译器、拦截器责任链、JDBC 模板、命名参数模板与批量模板，
 * 不依赖任何外部 DI 容器，使模块可在脱离 Spring 的环境中直接使用。
 *
 * <p>本上下文只提供模板方法（{@code JdbcTemplate} / {@code NamedParameterTemplate} /
 * {@code BatchTemplate} / {@code SqlRunner}），不提供 Repository 抽象；
 * 事务交由 Spring 声明式事务或外部事务管理器处理。
 *
 * <p>典型用法：
 * <pre>{@code
 * JdbcContext ctx = JdbcContext.builder()
 *         .properties(dataSourceProperties)
 *         .jdbc(jdbcProperties)
 *         .poolType(PoolType.HIKARI)
 *         .build();
 * List<Map<String, Object>> rows = ctx.getJdbcTemplate()
 *         .queryForList("select * from t_user where id = ?", 1L);
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class JdbcContext {

    private final JdbcProperties jdbcProperties;
    private final ConnectionProvider connectionProvider;
    private final DialectRegistry dialectRegistry;
    private final DefaultEntityMetadataResolver entityMetadataResolver;
    private final SQLExceptionTranslator exceptionTranslator;
    private final InterceptorChain interceptorChain;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterTemplate namedParameterTemplate;
    private final BatchTemplate batchTemplate;
    private final SqlRunner sqlRunner;

    JdbcContext(Builder builder) {
        this.jdbcProperties = builder.jdbcProperties;
        this.connectionProvider = builder.buildConnectionProvider();
        this.dialectRegistry = builder.dialectRegistry;
        this.entityMetadataResolver = new DefaultEntityMetadataResolver(builder.namingStrategy);
        this.exceptionTranslator = new SQLExceptionTranslator();
        this.interceptorChain = new InterceptorChain();
        if (builder.loggingInterceptor != null) {
            interceptorChain.addInterceptor(builder.loggingInterceptor);
        }
        if (builder.performanceInterceptor != null) {
            interceptorChain.addInterceptor(builder.performanceInterceptor);
        }
        if (builder.tenantInterceptor != null) {
            interceptorChain.addInterceptor(builder.tenantInterceptor);
        }
        for (SqlInterceptor extra : builder.extraInterceptors) {
            interceptorChain.addInterceptor(extra);
        }
        this.jdbcTemplate = new JdbcTemplate(connectionProvider, interceptorChain,
                DefaultTypeHandlers.getInstance(), exceptionTranslator);
        this.namedParameterTemplate = new NamedParameterTemplate(jdbcTemplate);
        this.batchTemplate = new BatchTemplate(jdbcTemplate);
        this.sqlRunner = new SqlRunner(jdbcTemplate);
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    public NamedParameterTemplate getNamedParameterTemplate() {
        return namedParameterTemplate;
    }

    public BatchTemplate getBatchTemplate() {
        return batchTemplate;
    }

    public SqlRunner getSqlRunner() {
        return sqlRunner;
    }

    public ConnectionProvider getConnectionProvider() {
        return connectionProvider;
    }

    public DialectRegistry getDialectRegistry() {
        return dialectRegistry;
    }

    public EntityMetadataResolver getEntityMetadataResolver() {
        return entityMetadataResolver;
    }

    public JdbcProperties getJdbcProperties() {
        return jdbcProperties;
    }

    /**
     * 关闭底层资源（连接池等）。
     */
    public void close() {
        connectionProvider.close();
    }

    /**
     * 流式构建器：以数据源属性、JDBC 属性、连接池类型等装配 {@link JdbcContext}。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * {@link JdbcContext} 的构建器。
     */
    @NullMarked
    public static final class Builder {
        private DataSourceProperties dataSourceProperties = new DataSourceProperties();
        private JdbcProperties jdbcProperties = new JdbcProperties();
        private PoolType poolType;
        private NamingStrategy namingStrategy = new CamelCaseNamingStrategy();
        private DialectRegistry dialectRegistry = new DialectRegistry();
        private @Nullable MeterRegistry meterRegistry;
        private LoggingInterceptor loggingInterceptor;
        private PerformanceInterceptor performanceInterceptor;
        private TenantInterceptor tenantInterceptor;
        private final List<SqlInterceptor> extraInterceptors = new ArrayList<>();

        public Builder properties(DataSourceProperties props) {
            this.dataSourceProperties = props;
            return this;
        }

        public Builder jdbc(JdbcProperties props) {
            this.jdbcProperties = props;
            return this;
        }

        public Builder poolType(PoolType poolType) {
            this.poolType = poolType;
            return this;
        }

        public Builder namingStrategy(NamingStrategy strategy) {
            this.namingStrategy = strategy;
            return this;
        }

        public Builder dialectRegistry(DialectRegistry registry) {
            this.dialectRegistry = registry;
            return this;
        }

        /**
         * 设置 Micrometer 指标注册中心（可选），用于 SQL 耗时上报。
         *
         * @param meterRegistry 指标注册中心，可为 {@code null}
         * @return 构建器本身
         */
        public Builder meterRegistry(@Nullable MeterRegistry meterRegistry) {
            this.meterRegistry = meterRegistry;
            return this;
        }

        public Builder addInterceptor(SqlInterceptor interceptor) {
            this.extraInterceptors.add(interceptor);
            return this;
        }

        public Builder enableSqlLog(boolean enabled) {
            this.jdbcProperties.setSqlLogEnabled(enabled);
            return this;
        }

        public Builder slowSqlThreshold(long millis) {
            this.jdbcProperties.setSlowSqlThreshold(millis);
            return this;
        }

        public Builder enableTenant(boolean enabled) {
            this.jdbcProperties.setTenantEnabled(enabled);
            return this;
        }

        public Builder tenantColumn(String column) {
            this.jdbcProperties.setTenantColumn(column);
            return this;
        }

        /**
         * 构建 {@link JdbcContext}。默认开启日志与性能拦截器；开启多租户时追加租户拦截器。
         *
         * @return 上下文实例
         */
        public JdbcContext build() {
            this.loggingInterceptor = new LoggingInterceptor(jdbcProperties);
            this.performanceInterceptor = new PerformanceInterceptor(jdbcProperties, meterRegistry);
            if (jdbcProperties.isTenantEnabled()) {
                this.tenantInterceptor = new TenantInterceptor(jdbcProperties);
            }
            return new JdbcContext(this);
        }

        ConnectionProvider buildConnectionProvider() {
            PoolType type = poolType != null ? poolType : dataSourceProperties.getPoolType();
            return switch (type) {
                case HIKARI -> new HikariConnectionProvider(dataSourceProperties);
                case DRUID -> new DruidConnectionProvider(dataSourceProperties);
                case NONE, SIMPLE -> new SimpleConnectionProvider(dataSourceProperties);
            };
        }
    }
}
