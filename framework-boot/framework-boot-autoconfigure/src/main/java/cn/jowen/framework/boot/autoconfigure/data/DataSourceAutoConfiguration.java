package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.data.core.datasource.DataSource;
import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.DataSourceRouter;
import cn.jowen.framework.data.core.datasource.DefaultDataSourceRouter;
import cn.jowen.framework.data.core.datasource.SimpleDataSource;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 数据源抽象装配：从 {@code framework.data.datasource.*} 构建 framework-data-core 的
 * {@link DataSource} 元信息与默认 {@link DataSourceRouter}（基于 {@link DefaultDataSourceRouter} + DataSourceContext 路由键）。
 *
 * <p>仅装配元信息与路由能力，不管理物理连接池——JDBC 路径的连接池由 {@link JdbcContext} 自管、
 * MyBatis 路径由 {@code MybatisAutoConfiguration} 自管（本装配仅在 {@code framework.data.type=jdbc} 或缺省时生效，
 * 与 {@code mybatis} 路径的路由实现互斥）。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "framework.data", name = "enabled", matchIfMissing = true)
@ConditionalOnProperty(prefix = "framework.data", name = "type", havingValue = "jdbc", matchIfMissing = true)
@EnableConfigurationProperties(BootDataSourceProperties.class)
public class DataSourceAutoConfiguration {

    /**
     * 主数据源元信息。
     *
     * @param properties 数据源配置属性，不可为 {@code null}
     * @return 主数据源元信息
     */
    @Bean("frameworkDataSource")
    @ConditionalOnMissingBean(DataSource.class)
    public DataSource frameworkDataSource(BootDataSourceProperties properties) {
        return SimpleDataSource.from(properties);
    }

    /**
     * 默认数据源路由器：主数据源 + 备用数据源集合（按 {@code secondary[*].name} 注册）。
     *
     * @param properties         数据源配置属性，不可为 {@code null}
     * @param frameworkDataSource 主数据源元信息，不可为 {@code null}
     * @return 数据源路由器
     */
    @Bean
    @ConditionalOnMissingBean(DataSourceRouter.class)
    public DataSourceRouter dataSourceRouter(BootDataSourceProperties properties,
                                             DataSource frameworkDataSource) {
        DefaultDataSourceRouter router = new DefaultDataSourceRouter(frameworkDataSource);
        for (DataSourceProperties secondary : properties.getSecondary()) {
            router.register(secondary.getName(), SimpleDataSource.from(secondary));
        }
        return router;
    }
}