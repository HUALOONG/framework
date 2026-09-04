package cn.jowen.framework.data.core.datasource;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SimpleDataSource} 测试：全量访问器、record 等值语义、工厂方法绑定，
 * 以及 {@link DataSource} 接口默认方法 {@code isInitialized} / {@code getProperties}。
 */
class SimpleDataSourceTest {

    private final SimpleDataSource source =
            new SimpleDataSource("primary", "jdbc:h2:mem:test", "app", "secret", PoolType.HIKARI);

    @Test
    void accessors_exposeAllComponents() {
        assertThat(source.getName()).isEqualTo("primary");
        assertThat(source.getUrl()).isEqualTo("jdbc:h2:mem:test");
        assertThat(source.getUsername()).isEqualTo("app");
        assertThat(source.getPassword()).isEqualTo("secret");
        assertThat(source.getPoolType()).isEqualTo(PoolType.HIKARI);
    }

    @Test
    void recordEquality_basedOnComponents() {
        SimpleDataSource same =
                new SimpleDataSource("primary", "jdbc:h2:mem:test", "app", "secret", PoolType.HIKARI);

        assertThat(source).isEqualTo(same).hasSameHashCodeAs(same);
        assertThat(source)
                .isNotEqualTo(new SimpleDataSource("other", "jdbc:h2:mem:test", "app", "secret", PoolType.HIKARI));
        assertThat(source)
                .isNotEqualTo(new SimpleDataSource("primary", "jdbc:h2:mem:test", "app", null, PoolType.HIKARI));
    }

    @Test
    void password_mayBeNull_whileOtherComponentsKept() {
        SimpleDataSource anonymous =
                new SimpleDataSource("anon", "jdbc:h2:mem:anon", "sa", null, PoolType.NONE);

        assertThat(anonymous.getPassword()).isNull();
        assertThat(anonymous.getUsername()).isEqualTo("sa");
        assertThat(anonymous.getPoolType()).isEqualTo(PoolType.NONE);
    }

    @Test
    void isInitialized_dependsOnNonBlankUrl() {
        assertThat(source.isInitialized()).isTrue();
        assertThat(new SimpleDataSource("empty", "", "sa", null, PoolType.SIMPLE).isInitialized()).isFalse();
        assertThat(new SimpleDataSource("blank", "   ", "sa", null, PoolType.SIMPLE).isInitialized()).isFalse();
    }

    @Test
    void getProperties_defaultsToEmptyMap() {
        assertThat(source.getProperties()).isEmpty();
    }

    @Test
    void getProperties_canBeOverriddenByImplementation() {
        DataSource custom = new DataSource() {
            @Override
            public String getName() {
                return "custom";
            }

            @Override
            public String getUrl() {
                return "jdbc:h2:mem:custom";
            }

            @Override
            public String getUsername() {
                return "sa";
            }

            @Override
            public String getPassword() {
                return null;
            }

            @Override
            public PoolType getPoolType() {
                return PoolType.HIKARI;
            }

            @Override
            public Map<String, String> getProperties() {
                return Map.of("cachePrepStmts", "true");
            }
        };

        assertThat(custom.getProperties()).containsEntry("cachePrepStmts", "true");
        assertThat(custom.isInitialized()).isTrue();
    }

    @Test
    void from_bindsAllProperties() {
        DataSourceProperties props = new DataSourceProperties();
        props.setName("report");
        props.setUrl("jdbc:h2:mem:report");
        props.setUsername("reader");
        props.setPassword("pwd");
        props.setPoolType(PoolType.DRUID);

        SimpleDataSource ds = SimpleDataSource.from(props);

        assertThat(ds.getName()).isEqualTo("report");
        assertThat(ds.getUrl()).isEqualTo("jdbc:h2:mem:report");
        assertThat(ds.getUsername()).isEqualTo("reader");
        assertThat(ds.getPassword()).isEqualTo("pwd");
        assertThat(ds.getPoolType()).isEqualTo(PoolType.DRUID);
    }

    @Test
    void from_defaultProperties_useDefaults() {
        SimpleDataSource ds = SimpleDataSource.from(new DataSourceProperties());

        assertThat(ds.getName()).isEqualTo("primary");
        assertThat(ds.getUrl()).isEmpty();
        assertThat(ds.getUsername()).isEmpty();
        assertThat(ds.getPassword()).isNull();
        assertThat(ds.getPoolType()).isEqualTo(PoolType.HIKARI);
    }

    @Test
    void from_nullProperties_throws() {
        assertThatThrownBy(() -> SimpleDataSource.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("properties");
    }
}
