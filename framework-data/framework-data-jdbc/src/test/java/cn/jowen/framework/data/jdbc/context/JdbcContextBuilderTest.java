package cn.jowen.framework.data.jdbc.context;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.mapping.NamingStrategy;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.connection.ConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.DataSourceConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.DruidConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.HikariConnectionProvider;
import cn.jowen.framework.data.jdbc.connection.SimpleConnectionProvider;
import cn.jowen.framework.data.jdbc.dialect.DialectRegistry;
import cn.jowen.framework.data.jdbc.interceptor.SqlInterceptor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link JdbcContext.Builder} 单元测试。
 */
class JdbcContextBuilderTest {

    private DataSourceProperties h2Properties() {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl("jdbc:h2:mem:ctx_builder;DB_CLOSE_DELAY=-1");
        properties.setUsername("sa");
        properties.setDriverClassName("org.h2.Driver");
        return properties;
    }

    private boolean druidAvailable() {
        try {
            Class.forName("com.alibaba.druid.pool.DruidDataSource");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Test
    void builder_simple_poolType() {
        JdbcContext.Builder builder = JdbcContext.builder()
                .properties(h2Properties())
                .poolType(PoolType.SIMPLE);
        ConnectionProvider provider = builder.buildConnectionProvider();
        assertThat(provider).isInstanceOf(SimpleConnectionProvider.class);
    }

    @Test
    void builder_hikari_poolType() {
        JdbcContext.Builder builder = JdbcContext.builder()
                .properties(h2Properties())
                .poolType(PoolType.HIKARI);
        ConnectionProvider provider = builder.buildConnectionProvider();
        assertThat(provider).isInstanceOf(HikariConnectionProvider.class);
    }

    @Test
    void builder_druid_poolType() {
        Assumptions.assumeTrue(druidAvailable());
        JdbcContext.Builder builder = JdbcContext.builder()
                .properties(h2Properties())
                .poolType(PoolType.DRUID);
        ConnectionProvider provider = builder.buildConnectionProvider();
        assertThat(provider).isInstanceOf(DruidConnectionProvider.class);
    }

    @Test
    void builder_poolTypeFromProperties() {
        DataSourceProperties properties = h2Properties();
        properties.setPoolType(PoolType.NONE);
        ConnectionProvider provider = JdbcContext.builder()
                .properties(properties)
                .buildConnectionProvider();
        assertThat(provider).isInstanceOf(SimpleConnectionProvider.class);
    }

    @Test
    void builder_defaultPoolType_hikari() {
        // DataSourceProperties 默认池类型为 HIKARI
        ConnectionProvider provider = JdbcContext.builder()
                .properties(h2Properties())
                .buildConnectionProvider();
        assertThat(provider).isInstanceOf(HikariConnectionProvider.class);
    }

    @Test
    void builder_setters_areFluent() {
        NamingStrategy naming = mock(NamingStrategy.class);
        DialectRegistry registry = new DialectRegistry();
        SqlInterceptor interceptor = mock(SqlInterceptor.class);

        JdbcContext.Builder builder = JdbcContext.builder()
                .properties(h2Properties())
                .jdbc(new JdbcProperties())
                .poolType(PoolType.SIMPLE)
                .namingStrategy(naming)
                .dialectRegistry(registry)
                .addInterceptor(interceptor)
                .enableSqlLog(true)
                .slowSqlThreshold(200)
                .enableTenant(false)
                .tenantColumn("tenant_id");

        assertThat(builder).isNotNull();
    }

    @Test
    void builder_enableTenant_createsTenantInterceptor() {
        JdbcContext context = JdbcContext.builder()
                .properties(h2Properties())
                .poolType(PoolType.SIMPLE)
                .enableTenant(true)
                .build();
        assertThat(context).isNotNull();
        assertThat(context.getJdbcProperties().isTenantEnabled()).isTrue();
        context.close();
    }

    @Test
    void build_createsContext() {
        JdbcContext context = JdbcContext.builder()
                .properties(h2Properties())
                .poolType(PoolType.SIMPLE)
                .build();
        assertThat(context.getJdbcTemplate()).isNotNull();
        assertThat(context.getNamedParameterTemplate()).isNotNull();
        assertThat(context.getBatchTemplate()).isNotNull();
        assertThat(context.getSqlRunner()).isNotNull();
        assertThat(context.getConnectionProvider()).isNotNull();
        assertThat(context.getDialectRegistry()).isNotNull();
        assertThat(context.getEntityMetadataResolver()).isNotNull();
        context.close();
    }
}
