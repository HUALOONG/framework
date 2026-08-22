package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.data.core.dialect.Dialect;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.core.repository.RepositoryFactory;
import cn.jowen.framework.data.core.transaction.TransactionManager;
import cn.jowen.framework.data.jdbc.dialect.StandardDialect;
import cn.jowen.framework.data.jdbc.mapping.DefaultEntityMetadataResolver;
import cn.jowen.framework.data.jdbc.repository.JdbcRepositoryFactory;
import cn.jowen.framework.data.jdbc.transaction.JdbcTransactionManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * JDBC 数据能力装配。复用 Spring Boot 原生 {@code DataSource}（由 spring-boot-starter-jdbc 的
 * {@code DataSourceAutoConfiguration} 负责），在其之上装配框架 core 抽象：
 * 实体元信息解析器、方言、core 事务管理器与仓储工厂。
 *
 * <p>所有 Bean 均带 {@link ConditionalOnMissingBean}，允许用户自定义覆盖；依赖 {@link DataSource} Bean 存在，
 * 因此必须晚于 Spring Boot 原生数据源装配生效（通过 {@code afterName} 指定顺序，避免对 spring-boot-jdbc 强耦合 import）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration(
        after = BootAutoConfiguration.class,
        afterName = "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
@ConditionalOnClass(name = {
        "org.springframework.jdbc.core.JdbcTemplate",
        "cn.jowen.framework.data.jdbc.repository.JdbcRepositoryFactory"})
@ConditionalOnBean(DataSource.class)
@ConditionalOnProperty(prefix = "framework.data", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataJdbcAutoConfiguration {

    /**
     * 实体元信息解析器（基于 {@code meta} 注解反射，带缓存）。
     *
     * @return 解析器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public EntityMetadataResolver entityMetadataResolver() {
        return new DefaultEntityMetadataResolver();
    }

    /**
     * SQL 方言。当前默认 {@link StandardDialect}（H2/MySQL/PostgreSQL 兼容）。
     *
     * @param properties 数据源属性，不可为 {@code null}
     * @return 方言，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public Dialect dataDialect(DataSourceProperties properties) {
        String dialect = properties.getDialect();
        if ("standard".equalsIgnoreCase(dialect)) {
            return new StandardDialect();
        }
        throw new IllegalArgumentException("不支持的框架方言：" + dialect);
    }

    /**
     * core 事务管理器，桥接 Spring {@code DataSourceTransactionManager}。
     *
     * @param dataSource 数据源，不可为 {@code null}
     * @return 事务管理器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public TransactionManager frameworkTransactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    /**
     * 仓储工厂，基于 {@link JdbcTemplate} 创建通用 JDBC 仓储。
     *
     * @param dataSource 数据源，不可为 {@code null}
     * @param resolver   实体元信息解析器，不可为 {@code null}
     * @param dialect    方言，不可为 {@code null}
     * @return 仓储工厂，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public RepositoryFactory jdbcRepositoryFactory(DataSource dataSource,
                                                   EntityMetadataResolver resolver,
                                                   Dialect dialect) {
        return new JdbcRepositoryFactory(dataSource, resolver, dialect);
    }
}
