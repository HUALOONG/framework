package cn.jowen.framework.data.jdbc.context;

import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.core.dialect.DatabaseDialect;
import cn.jowen.framework.data.core.dialect.DatabaseType;
import cn.jowen.framework.data.jdbc.dialect.H2Dialect;
import cn.jowen.framework.data.jdbc.dialect.MySQLDialect;
import cn.jowen.framework.data.jdbc.interceptor.SqlInterceptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link JdbcContextBuilder} 门面单元测试。
 */
class JdbcContextBuilderFacadeTest {

    @Test
    void create_returnsBuilder() {
        assertThat(JdbcContextBuilder.create()).isNotNull();
    }

    @Test
    void fluentSetters_areChainable() {
        SqlInterceptor interceptor = mock(SqlInterceptor.class);
        JdbcContextBuilder builder = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:facade;DB_CLOSE_DELAY=-1")
                .username("sa")
                .password("")
                .driverClassName("org.h2.Driver")
                .poolType(PoolType.SIMPLE)
                .addInterceptor(interceptor)
                .tenantEnabled(false);

        assertThat(builder).isNotNull();
    }

    @Test
    void build_createsContext() {
        JdbcContext context = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:facade;DB_CLOSE_DELAY=-1")
                .username("sa")
                .driverClassName("org.h2.Driver")
                .poolType(PoolType.SIMPLE)
                .build();

        assertThat(context).isNotNull();
        assertThat(context.getJdbcTemplate()).isNotNull();
        context.close();
    }

    @Test
    void dialect_byType_registersDialect() {
        JdbcContext context = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:facade;DB_CLOSE_DELAY=-1")
                .username("sa")
                .driverClassName("org.h2.Driver")
                .poolType(PoolType.SIMPLE)
                .dialect(DatabaseType.MYSQL)
                .build();

        DatabaseDialect dialect = context.getDialectRegistry().getDefault();
        assertThat(dialect).isInstanceOf(MySQLDialect.class);
        context.close();
    }

    @Test
    void dialect_nullType_usesDefault() {
        JdbcContext context = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:facade;DB_CLOSE_DELAY=-1")
                .username("sa")
                .driverClassName("org.h2.Driver")
                .poolType(PoolType.SIMPLE)
                .dialect((DatabaseType) null)
                .build();

        assertThat(context.getDialectRegistry().getDefault()).isInstanceOf(H2Dialect.class);
        context.close();
    }

    @Test
    void dialect_byInstance_registersDialect() {
        MySQLDialect mysql = new MySQLDialect();
        JdbcContext context = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:facade;DB_CLOSE_DELAY=-1")
                .username("sa")
                .driverClassName("org.h2.Driver")
                .poolType(PoolType.SIMPLE)
                .dialect(mysql)
                .build();

        assertThat(context.getDialectRegistry().getDefault()).isSameAs(mysql);
        context.close();
    }

    @Test
    void tenantEnabled_createsTenantInterceptor() {
        JdbcContext context = JdbcContextBuilder.create()
                .url("jdbc:h2:mem:facade;DB_CLOSE_DELAY=-1")
                .username("sa")
                .driverClassName("org.h2.Driver")
                .poolType(PoolType.SIMPLE)
                .tenantEnabled(true)
                .build();

        assertThat(context.getJdbcProperties().isTenantEnabled()).isTrue();
        context.close();
    }
}
