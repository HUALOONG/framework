package cn.jowen.framework.data.jdbc.context;

import cn.jowen.framework.data.core.datasource.DataSourceProperties;
import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.core.mapping.EntityMetadataResolver;
import cn.jowen.framework.data.jdbc.config.JdbcProperties;
import cn.jowen.framework.data.jdbc.mapping.CamelCaseNamingStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JdbcContext} 组件装配与访问器单元测试。
 */
class JdbcContextTest {

    private static final String DDL = """
            CREATE TABLE IF NOT EXISTS app_user (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(50),
                age INT
            );
            """;

    private JdbcContext buildContext() {
        JdbcContext context = JdbcContext.builder()
                .properties(new DataSourceProperties() {{
                    setUrl("jdbc:h2:mem:ctx;DB_CLOSE_DELAY=-1");
                    setUsername("sa");
                    setPassword("");
                    setDriverClassName("org.h2.Driver");
                }})
                .poolType(PoolType.SIMPLE)
                .build();
        context.getJdbcTemplate().execute(DDL);
        return context;
    }

    @Test
    void getters_returnAssembledComponents() {
        JdbcContext context = buildContext();
        try {
            assertThat(context.getJdbcTemplate()).isNotNull();
            assertThat(context.getNamedParameterTemplate()).isNotNull();
            assertThat(context.getBatchTemplate()).isNotNull();
            assertThat(context.getSqlRunner()).isNotNull();
            assertThat(context.getConnectionProvider()).isNotNull();
            assertThat(context.getDialectRegistry()).isNotNull();
            assertThat(context.getJdbcProperties()).isNotNull();
            EntityMetadataResolver resolver = context.getEntityMetadataResolver();
            assertThat(resolver).isNotNull();
        } finally {
            context.close();
        }
    }

    @Test
    void builder_customSettings_propagate() {
        JdbcProperties props = new JdbcProperties();
        props.setTenantColumn("org_id");
        JdbcContext context = JdbcContext.builder()
                .properties(new DataSourceProperties() {{
                    setUrl("jdbc:h2:mem:ctx2;DB_CLOSE_DELAY=-1");
                    setUsername("sa");
                    setPassword("");
                    setDriverClassName("org.h2.Driver");
                }})
                .poolType(PoolType.SIMPLE)
                .jdbc(props)
                .namingStrategy(new CamelCaseNamingStrategy())
                .dialectRegistry(new cn.jowen.framework.data.jdbc.dialect.DialectRegistry())
                .enableSqlLog(true)
                .slowSqlThreshold(500L)
                .enableTenant(true)
                .tenantColumn("org_id")
                .build();
        try {
            assertThat(context.getJdbcProperties().getTenantColumn()).isEqualTo("org_id");
            assertThat(context.getJdbcProperties().getSlowSqlThreshold()).isEqualTo(500L);
            assertThat(context.getJdbcProperties().isSqlLogEnabled()).isTrue();
            assertThat(context.getJdbcProperties().isTenantEnabled()).isTrue();
        } finally {
            context.close();
        }
    }

    @Test
    void builder_defaultDialect_registersTypes() {
        JdbcContext context = JdbcContext.builder()
                .properties(new DataSourceProperties() {{
                    setUrl("jdbc:h2:mem:ctx3;DB_CLOSE_DELAY=-1");
                    setUsername("sa");
                    setPassword("");
                    setDriverClassName("org.h2.Driver");
                }})
                .poolType(PoolType.SIMPLE)
                .build();
        try {
            assertThat(context.getDialectRegistry().get(DatabaseType.H2)).isNotNull();
            assertThat(context.getDialectRegistry().get(DatabaseType.MYSQL)).isNotNull();
        } finally {
            context.close();
        }
    }

    @Test
    void meterRegistry_buildsPerformanceInterceptor() {
        io.micrometer.core.instrument.simple.SimpleMeterRegistry registry =
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry();
        JdbcContext context = JdbcContext.builder()
                .properties(new DataSourceProperties() {{
                    setUrl("jdbc:h2:mem:ctx5;DB_CLOSE_DELAY=-1");
                    setUsername("sa");
                    setPassword("");
                    setDriverClassName("org.h2.Driver");
                }})
                .poolType(PoolType.SIMPLE)
                .meterRegistry(registry)
                .build();
        try {
            context.getJdbcTemplate().queryForObject("SELECT 1", Integer.class);
            assertThat(registry.find("framework.jdbc.sql").timer()).isNotNull();
        } finally {
            context.close();
        }
    }

    @Test
    void addInterceptor_registersExtraInterceptor() {
        java.util.concurrent.atomic.AtomicInteger count =
                new java.util.concurrent.atomic.AtomicInteger();
        JdbcContext context = JdbcContext.builder()
                .properties(new DataSourceProperties() {{
                    setUrl("jdbc:h2:mem:ctx6;DB_CLOSE_DELAY=-1");
                    setUsername("sa");
                    setPassword("");
                    setDriverClassName("org.h2.Driver");
                }})
                .poolType(PoolType.SIMPLE)
                .addInterceptor(new cn.jowen.framework.data.jdbc.interceptor.SqlInterceptor() {
                    @Override
                    public Object intercept(cn.jowen.framework.data.jdbc.interceptor.SqlContext sqlCtx,
                                            cn.jowen.framework.data.jdbc.interceptor.InterceptorChain chain) {
                        count.incrementAndGet();
                        return sqlCtx.proceed();
                    }
                })
                .build();
        try {
            context.getJdbcTemplate().execute("SELECT 1");
            assertThat(count.get()).isEqualTo(1);
        } finally {
            context.close();
        }
    }
}
