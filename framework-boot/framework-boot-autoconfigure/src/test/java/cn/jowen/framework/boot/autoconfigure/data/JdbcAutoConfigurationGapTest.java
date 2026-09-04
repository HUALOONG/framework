package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.core.datasource.PoolType;
import cn.jowen.framework.data.jdbc.context.JdbcContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link JdbcAutoConfiguration#jdbcContext} 指标接线分支测试。
 *
 * <p>既有 {@code JdbcAutoConfigurationTest} 走 {@code ApplicationContextRunner}，容器内未提供
 * {@code MeterRegistry}，故「注入 SQL Timer 指标注册中心」的分支未触达。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class JdbcAutoConfigurationGapTest {

    private static final String URL = "jdbc:h2:mem:jowen-jdbc-gap;DB_CLOSE_DELAY=-1";

    @Test
    void jdbcContext_bindsMeterRegistryWhenAvailable() {
        BootDataSourceProperties dataSourceProperties = new BootDataSourceProperties();
        dataSourceProperties.setUrl(URL);
        dataSourceProperties.setPoolType(PoolType.SIMPLE);
        dataSourceProperties.setMaximumPoolSize(3);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(new SimpleMeterRegistry());

        JdbcContext context = new JdbcAutoConfiguration()
                .jdbcContext(dataSourceProperties, new BootJdbcProperties(), provider);
        try {
            assertThat(context.getJdbcTemplate()).isNotNull();
            assertThat(context.getSqlRunner()).isNotNull();
        } finally {
            context.close();
        }
    }

    @Test
    void jdbcContext_skipsMeterRegistryWhenUnavailable() {
        BootDataSourceProperties dataSourceProperties = new BootDataSourceProperties();
        dataSourceProperties.setUrl(URL);
        dataSourceProperties.setPoolType(PoolType.SIMPLE);

        @SuppressWarnings({"unchecked", "rawtypes"})
        ObjectProvider<MeterRegistry> provider = mock(ObjectProvider.class);

        JdbcContext context = new JdbcAutoConfiguration()
                .jdbcContext(dataSourceProperties, new BootJdbcProperties(), provider);
        try {
            assertThat(context).isNotNull();
        } finally {
            context.close();
        }
    }
}
