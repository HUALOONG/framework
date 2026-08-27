package cn.jowen.framework.data.core.datasource;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class DataSourcePropertiesTest {

    @Test
    void defaultValues() {
        DataSourceProperties props = new DataSourceProperties();
        assertThat(props.getName()).isEqualTo("primary");
        assertThat(props.getUrl()).isEmpty();
        assertThat(props.getUsername()).isEmpty();
        assertThat(props.getPassword()).isNull();
        assertThat(props.getDriverClassName()).isNull();
        assertThat(props.getPoolType()).isEqualTo(PoolType.HIKARI);
        assertThat(props.getMaximumPoolSize()).isEqualTo(20);
        assertThat(props.getConnectionTimeout()).isEqualTo(5000);
        assertThat(props.getIdleTimeout()).isEqualTo(600000);
        assertThat(props.getMaxLifetime()).isEqualTo(1800000);
        assertThat(props.isReadOnly()).isFalse();
        assertThat(props.getSecondary()).isEmpty();
    }

    @Test
    void setters() {
        DataSourceProperties props = new DataSourceProperties();
        props.setName("replica");
        props.setUrl("jdbc:mysql://localhost:3306/test");
        props.setUsername("root");
        props.setPassword("secret");
        props.setDriverClassName("com.mysql.cj.jdbc.Driver");
        props.setPoolType(PoolType.DRUID);
        props.setMaximumPoolSize(50);
        props.setConnectionTimeout(10000);
        props.setIdleTimeout(300000);
        props.setMaxLifetime(900000);
        props.setReadOnly(true);
        props.setSecondary(List.of());

        assertThat(props.getName()).isEqualTo("replica");
        assertThat(props.getUrl()).isEqualTo("jdbc:mysql://localhost:3306/test");
        assertThat(props.getUsername()).isEqualTo("root");
        assertThat(props.getPassword()).isEqualTo("secret");
        assertThat(props.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(props.getPoolType()).isEqualTo(PoolType.DRUID);
        assertThat(props.getMaximumPoolSize()).isEqualTo(50);
        assertThat(props.getConnectionTimeout()).isEqualTo(10000);
        assertThat(props.getIdleTimeout()).isEqualTo(300000);
        assertThat(props.getMaxLifetime()).isEqualTo(900000);
        assertThat(props.isReadOnly()).isTrue();
    }

    @Test
    void passwordCanBeNulled() {
        DataSourceProperties props = new DataSourceProperties();
        props.setPassword("secret");
        props.setPassword(null);
        assertThat(props.getPassword()).isNull();
    }
}
