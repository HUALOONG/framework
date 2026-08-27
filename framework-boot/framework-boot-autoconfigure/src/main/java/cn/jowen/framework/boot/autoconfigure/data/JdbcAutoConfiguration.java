package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.core.transaction.TransactionTemplate;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import cn.jowen.framework.data.jdbc.core.BatchTemplate;
import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.data.jdbc.core.NamedParameterTemplate;
import cn.jowen.framework.data.jdbc.core.SqlRunner;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.repository.DefaultIdGenerator;
import cn.jowen.framework.data.jdbc.repository.IdGenerator;
import cn.jowen.framework.data.jdbc.repository.JdbcRepositoryFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * JDBC 数据层自动装配（M2）。
 *
 * <p>默认数据层实现（{@code framework.data.type=jdbc}，缺省即 jdbc）。装配流程：
 * <ol>
 *     <li>绑定 {@code framework.data.datasource.*} 数据源属性与 {@code framework.data.jdbc.*} JDBC 属性；</li>
 *     <li>以 {@link JdbcContext} 为轻量 IoC 上下文装配连接池、方言、实体元信息解析器、
 *         拦截器责任链（日志/性能/租户）、JDBC 模板与事务管理器；</li>
 *     <li>向 Spring 容器暴露 {@link JdbcTemplate}、{@link RepositoryFactory}、{@link TransactionManager}、
 *         {@link TransactionTemplate}、{@link DialectRegistry} 等组件，供业务方注入使用。</li>
 * </ol>
 *
 * <p>典型配置：
 * <pre>{@code
 * framework:
 *   data:
 *     type: jdbc            # 缺省即 jdbc
 *     enabled: true         # 数据层总开关，缺省开启
 *     datasource:
 *       url: jdbc:mysql://localhost:3306/app
 *       username: root
 *       password: root
 *       pool-type: HIKARI     # HIKARI | DRUID | SIMPLE
 *     jdbc:
 *       sql-log-enabled: true
 *       slow-sql-threshold: 200
 *       tenant-enabled: false
 * }</pre>
 *
 * <p>关闭方式：{@code framework.data.enabled=false} 或 {@code framework.data.type=mybatis}。
 *
 * @author 王飞
 * @since 2026-08-26
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(JdbcContext.class)
@ConditionalOnProperty(prefix = "framework.data", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "framework.data", name = "type", havingValue = "jdbc", matchIfMissing = true)
@EnableConfigurationProperties({BootDataSourceProperties.class, BootJdbcProperties.class})
public class JdbcAutoConfiguration {

    /**
     * 轻量 IoC 上下文：聚合连接提供者、方言、实体元信息解析器、拦截器、模板与事务管理器。
     *
     * @param dataSourceProperties 数据源属性（{@code framework.data.*}）
     * @param jdbcProperties       JDBC 属性（{@code framework.data.jdbc.*}）
     * @param idGenerator          可选的主键生成器，缺省使用 {@link DefaultIdGenerator}
     * @return JDBC 上下文
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public JdbcContext jdbcContext(BootDataSourceProperties dataSourceProperties,
                                   BootJdbcProperties jdbcProperties,
                                   ObjectProvider<IdGenerator> idGenerator,
                                   ObjectProvider<MeterRegistry> meterRegistry) {
        JdbcContext.Builder builder = JdbcContext.builder()
                .properties(dataSourceProperties)
                .jdbc(jdbcProperties);
        IdGenerator idGen = idGenerator.getIfAvailable();
        if (idGen != null) {
            builder.idGenerator(idGen);
        }
        MeterRegistry registry = meterRegistry.getIfAvailable();
        if (registry != null) {
            builder.meterRegistry(registry);
        }
        return builder.build();
    }

    /**
     * 主键生成器缺省实现；业务方可通过 {@link IdGenerator} Bean 覆盖。
     *
     * @return 默认主键生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public IdGenerator idGenerator() {
        return new DefaultIdGenerator();
    }

    /**
     * 轻量 JDBC 模板（框架自带，零 Spring 依赖）。
     *
     * @param context JDBC 上下文
     * @return JDBC 模板
     */
    @Bean
    @ConditionalOnMissingBean
    public JdbcTemplate jdbcTemplate(JdbcContext context) {
        return context.getJdbcTemplate();
    }

    /**
     * 命名参数模板（{@code :name} 占位符）。
     *
     * @param context JDBC 上下文
     * @return 命名参数模板
     */
    @Bean
    @ConditionalOnMissingBean
    public NamedParameterTemplate namedParameterTemplate(JdbcContext context) {
        return context.getNamedParameterTemplate();
    }

    /**
     * 批量执行模板。
     *
     * @param context JDBC 上下文
     * @return 批量模板
     */
    @Bean
    @ConditionalOnMissingBean
    public BatchTemplate batchTemplate(JdbcContext context) {
        return context.getBatchTemplate();
    }

    /**
     * SQL 运行器（语句级执行入口）。
     *
     * @param context JDBC 上下文
     * @return SQL 运行器
     */
    @Bean
    @ConditionalOnMissingBean
    public SqlRunner sqlRunner(JdbcContext context) {
        return context.getSqlRunner();
    }

    /**
     * 仓储工厂：按实体类型获取 {@link cn.jowen.framework.data.core.repository.Repository}。
     * 与 {@link JdbcContext} 内部工厂共用同一模板、方言与主键生成器，保证行为一致。
     *
     * @param context JDBC 上下文
     * @return 仓储工厂
     */
    @Bean
    @ConditionalOnMissingBean
    public RepositoryFactory repositoryFactory(JdbcContext context, IdGenerator idGenerator) {
        return new JdbcRepositoryFactory(context.getJdbcTemplate(), context.getEntityMetadataResolver(),
                context.getDialectRegistry(), idGenerator);
    }

    /**
     * 事务管理器（框架自有事务抽象）。
     *
     * @param context JDBC 上下文
     * @return 事务管理器
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionManager transactionManager(JdbcContext context) {
        return context.getTransactionManager();
    }

    /**
     * 编程式事务模板：成功提交、运行时异常回滚。
     *
     * @param transactionManager 事务管理器
     * @return 事务模板
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionTemplate transactionTemplate(TransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }

    /**
     * 实体元信息解析器：解析实体类到表/列/主键映射。
     *
     * @param context JDBC 上下文
     * @return 实体元信息解析器
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityMetadataResolver entityMetadataResolver(JdbcContext context) {
        return context.getEntityMetadataResolver();
    }

    /**
     * 方言注册中心：维护数据库类型 → 方言映射。
     *
     * @param context JDBC 上下文
     * @return 方言注册中心
     */
    @Bean
    @ConditionalOnMissingBean
    public DialectRegistry dialectRegistry(JdbcContext context) {
        return context.getDialectRegistry();
    }

    /**
     * 连接提供者：管理数据库连接池生命周期。
     *
     * @param context JDBC 上下文
     * @return 连接提供者
     */
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public ConnectionProvider connectionProvider(JdbcContext context) {
        return context.getConnectionProvider();
    }

    /**
     * 默认方言。
     *
     * @param dialectRegistry 方言注册中心
     * @return 默认方言
     */
    @Bean
    @ConditionalOnMissingBean
    public DatabaseDialect databaseDialect(DialectRegistry dialectRegistry) {
        return dialectRegistry.getDefault();
    }
}
