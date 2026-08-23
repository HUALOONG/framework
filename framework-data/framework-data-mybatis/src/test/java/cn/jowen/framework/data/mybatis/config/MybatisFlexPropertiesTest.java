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
}
