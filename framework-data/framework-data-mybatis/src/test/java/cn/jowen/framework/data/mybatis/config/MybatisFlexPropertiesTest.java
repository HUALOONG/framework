package cn.jowen.framework.data.mybatis.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MybatisFlexPropertiesTest {

    @Test
    void defaultValues() {
        MybatisFlexProperties props = new MybatisFlexProperties();
        assertThat(props.getUrl()).isEmpty();
        assertThat(props.getUsername()).isEmpty();
        assertThat(props.getPassword()).isNull();
        assertThat(props.getMaximumPoolSize()).isEqualTo(20);
        assertThat(props.getConnectionTimeout()).isEqualTo(5000L);
        assertThat(props.getMapperLocations()).isEqualTo("classpath*:/mapper/**/*Mapper.xml");
        assertThat(props.isAuditEnabled()).isFalse();
        assertThat(props.isEncryptEnabled()).isFalse();
        assertThat(props.isTenantEnabled()).isFalse();
        assertThat(props.isSqlAuditEnabled()).isFalse();
        assertThat(props.isLogicDeleteEnabled()).isFalse();
        assertThat(props.isOptimisticLockEnabled()).isFalse();
    }

    @Test
    void setters() {
        MybatisFlexProperties props = new MybatisFlexProperties();
        props.setUrl("jdbc:mysql://localhost:3306/db");
        props.setUsername("root");
        props.setPassword("secret");
        props.setMaximumPoolSize(50);
        props.setAuditEnabled(true);
        assertThat(props.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/db");
        assertThat(props.getUsername()).isEqualTo("root");
        assertThat(props.getPassword()).isEqualTo("secret");
        assertThat(props.getMaximumPoolSize()).isEqualTo(50);
        assertThat(props.isAuditEnabled()).isTrue();
    }

    @Test
    void flexGlobalConfigCustomizer() {
        assertThatCode(() -> {
            FlexGlobalConfigCustomizer customizer = config -> {
                // no-op
            };
            customizer.customize(new Object());
        }).doesNotThrowAnyException();
    }

    @Test
    void allSettersAreIdempotent() {
        // 补齐 setter 覆盖：driverClassName / connectionTimeout / mapperLocations /
        // typeAliasesPackage / encryptEnabled / tenantEnabled / sqlAuditEnabled /
        // logicDeleteEnabled / optimisticLockEnabled
        MybatisFlexProperties props = new MybatisFlexProperties();
        props.setDriverClassName("com.mysql.cj.jdbc.Driver");
        props.setConnectionTimeout(1234L);
        props.setMapperLocations("classpath:/foo/Mapper.xml");
        props.setTypeAliasesPackage("cn.jowen.app.model");
        props.setEncryptEnabled(true);
        props.setTenantEnabled(true);
        props.setSqlAuditEnabled(true);
        props.setLogicDeleteEnabled(true);
        props.setOptimisticLockEnabled(true);
        assertThat(props.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(props.getConnectionTimeout()).isEqualTo(1234L);
        assertThat(props.getMapperLocations()).isEqualTo("classpath:/foo/Mapper.xml");
        assertThat(props.getTypeAliasesPackage()).isEqualTo("cn.jowen.app.model");
        assertThat(props.isEncryptEnabled()).isTrue();
        assertThat(props.isTenantEnabled()).isTrue();
        assertThat(props.isSqlAuditEnabled()).isTrue();
        assertThat(props.isLogicDeleteEnabled()).isTrue();
        assertThat(props.isOptimisticLockEnabled()).isTrue();
    }
}
