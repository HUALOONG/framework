package cn.jowen.framework.data.mybatis.adapter;

import cn.jowen.framework.data.core.datasource.DataSource;
import cn.jowen.framework.data.core.datasource.DataSourceContext;
import cn.jowen.framework.data.core.datasource.PoolType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlexDataSourceAdapterTest {

    private DataSource ds(String name) {
        return new DataSource() {
            @Override public String getName() { return name; }
            @Override public String getUrl() { return "jdbc:h2:" + name; }
            @Override public String getUsername() { return "u"; }
            @Override public String getPassword() { return null; }
            @Override public PoolType getPoolType() { return PoolType.SIMPLE; }
        };
    }

    @Test
    void constructor_nullDynamicDataSource_throws() {
        assertThatThrownBy(() -> new FlexDataSourceAdapter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dynamicDataSource must not be null");
    }

    @Test
    void getDataSource_returnsFirstRegisteredOrDefault() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new Object());
        DataSource primary = ds("primary");
        adapter.registerDataSource("primary", primary);
        assertThat(adapter.getDataSource()).isSameAs(primary);
    }

    @Test
    void getDataSourceByName_findsAndMisses() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new Object());
        DataSource a = ds("a");
        adapter.registerDataSource("a", a);
        assertThat(adapter.getDataSource("a")).isSameAs(a);
        assertThat(adapter.getDataSource("missing")).isNull();
    }

    @Test
    void getAllDataSources_returnsRegistered() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new Object());
        adapter.registerDataSource("a", ds("a"));
        assertThat(adapter.getAllDataSources()).hasSize(1);
    }

    @Test
    void registerDataSource_nullArgs_throws() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new Object());
        assertThatThrownBy(() -> adapter.registerDataSource(null, ds("x")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> adapter.registerDataSource("x", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setCurrentDataSource_invokesAndClears() {
        FlexDataSourceAdapter adapter = new FlexDataSourceAdapter(new Object());
        assertThatThrownBy(() -> adapter.setCurrentDataSource(null))
                .isInstanceOf(IllegalArgumentException.class);
        // setCurrentDataSource / clearCurrentDataSource 经 DataSourceContext 写入 ScopedValue，
        // 必须在 runWith 作用域内调用；任意对象无 determineCurrentLookupKey 方法，走 debug 分支
        DataSourceContext.runWith("ds1", () -> {
            adapter.setCurrentDataSource("ds1");
            assertThat(DataSourceContext.getDataSourceKey()).isEqualTo("ds1");
            adapter.clearCurrentDataSource();
            assertThat(DataSourceContext.getDataSourceKey()).isNull();
        });
    }
}
