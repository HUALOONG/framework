package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.data.core.datasource.PoolType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BootDataSourceProperties}、{@link BootJdbcProperties} 与 {@link BootMybatisFlexProperties} 测试。
 */
class BootDataPropertiesTest {

    @Test
    void dataSourceProperties_gettersAndSetters() {
        BootDataSourceProperties properties = new BootDataSourceProperties();
        properties.setUrl("jdbc:h2:mem:test");
        properties.setUsername("sa");
        properties.setPassword("secret");
        properties.setPoolType(PoolType.SIMPLE);
        assertThat(properties.getUrl()).isEqualTo("jdbc:h2:mem:test");
        assertThat(properties.getUsername()).isEqualTo("sa");
        assertThat(properties.getPassword()).isEqualTo("secret");
        assertThat(properties.getPoolType()).isEqualTo(PoolType.SIMPLE);
    }

    @Test
    void jdbcProperties_gettersAndSetters() {
        BootJdbcProperties properties = new BootJdbcProperties();
        properties.setSqlLogEnabled(false);
        properties.setSlowSqlThreshold(300L);
        properties.setTenantEnabled(true);
        assertThat(properties.isSqlLogEnabled()).isFalse();
        assertThat(properties.getSlowSqlThreshold()).isEqualTo(300L);
        assertThat(properties.isTenantEnabled()).isTrue();
    }

    @Test
    void mybatisFlexProperties_gettersAndSetters() {
        BootMybatisFlexProperties properties = new BootMybatisFlexProperties();
        properties.setUrl("jdbc:h2:mem:test");
        properties.setMaximumPoolSize(30);
        properties.setConnectionTimeout(8000L);
        properties.setAuditEnabled(true);
        properties.setEncryptEnabled(true);
        properties.setLogicDeleteEnabled(true);
        properties.setOptimisticLockEnabled(true);
        assertThat(properties.getUrl()).isEqualTo("jdbc:h2:mem:test");
        assertThat(properties.getMaximumPoolSize()).isEqualTo(30);
        assertThat(properties.getConnectionTimeout()).isEqualTo(8000L);
        assertThat(properties.isAuditEnabled()).isTrue();
        assertThat(properties.isEncryptEnabled()).isTrue();
        assertThat(properties.isLogicDeleteEnabled()).isTrue();
        assertThat(properties.isOptimisticLockEnabled()).isTrue();
    }
}
