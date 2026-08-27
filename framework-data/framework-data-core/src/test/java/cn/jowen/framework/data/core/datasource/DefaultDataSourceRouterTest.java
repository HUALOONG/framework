package cn.jowen.framework.data.core.datasource;

import cn.jowen.framework.core.context.ContextCarrier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DefaultDataSourceRouter} 测试：默认回退、按名路由、当前键切换与参数校验。
 */
class DefaultDataSourceRouterTest {

    private final DataSource primary = new SimpleDataSource("primary", "jdbc:h2:mem:main", "sa", null, PoolType.HIKARI);
    private final DataSource backup = new SimpleDataSource("backup", "jdbc:h2:mem:backup", "sa", null, PoolType.HIKARI);

    private DefaultDataSourceRouter router;

    @BeforeEach
    void setUp() {
        // ScopedValue 模式下 set() 须在 runWith 作用域内调用，路由测试强制 ThreadLocal 模式（项目既定测试策略）
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
        router = new DefaultDataSourceRouter(primary);
        router.register("backup", backup);
    }

    @Test
    void defaultRouting_returnsPrimary() {
        assertThat(router.getDataSource()).isSameAs(primary);
    }

    @Test
    void namedLookup_findsRegistered() {
        assertThat(router.getDataSource("backup")).isSameAs(backup);
        assertThat(router.getDataSource("missing")).isNull();
    }

    @Test
    void currentKey_switchesRouting() {
        router.setCurrentDataSource("backup");
        assertThat(router.getDataSource()).isSameAs(backup);

        router.clearCurrentDataSource();
        assertThat(router.getDataSource()).isSameAs(primary);
    }

    @Test
    void setCurrentDataSource_unknown_throws() {
        assertThatThrownBy(() -> router.setCurrentDataSource("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知数据源");
    }

    @Test
    void getAllDataSources_containsRegistered() {
        assertThat(router.getAllDataSources()).hasSize(2);
    }

    @Test
    void simpleDataSource_fromProperties_binds() {
        DataSourceProperties props = new DataSourceProperties();
        props.setName("custom");
        props.setUrl("jdbc:h2:mem:test");
        props.setUsername("sa");
        props.setPassword("secret");
        DataSource ds = SimpleDataSource.from(props);
        assertThat(ds.getName()).isEqualTo("custom");
        assertThat(ds.getUrl()).isEqualTo("jdbc:h2:mem:test");
        assertThat(ds.getPassword()).isEqualTo("secret");
        assertThat(ds.isInitialized()).isTrue();
    }

    @Test
    void nullArguments_throw() {
        assertThatThrownBy(() -> new DefaultDataSourceRouter(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.register("x", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> router.register(" ", primary))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SimpleDataSource.from(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}