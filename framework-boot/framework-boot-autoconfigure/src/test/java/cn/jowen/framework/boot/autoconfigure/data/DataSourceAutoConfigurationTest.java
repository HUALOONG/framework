package cn.jowen.framework.boot.autoconfigure.data;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.data.core.datasource.DataSource;
import cn.jowen.framework.data.core.datasource.DataSourceRouter;
import cn.jowen.framework.data.core.datasource.DefaultDataSourceRouter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DataSourceAutoConfiguration} 装配测试：数据源抽象与默认路由装配。
 */
class DataSourceAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class));

    @Test
    void defaultContext_registersAbstractionBeans() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(DataSource.class);
            assertThat(ctx).hasSingleBean(DataSourceRouter.class);
            assertThat(ctx.getBean(DataSourceRouter.class)).isInstanceOf(DefaultDataSourceRouter.class);
            assertThat(ctx.getBean(DataSource.class).getName()).isEqualTo("primary");
        });
    }

    @Test
    void secondarySources_registeredAndRoutable() {
        context.withPropertyValues(
                        "framework.data.datasource.url=jdbc:h2:mem:main",
                        "framework.data.datasource.secondary[0].name=backup",
                        "framework.data.datasource.secondary[0].url=jdbc:h2:mem:backup")
                .run(ctx -> {
                    // ScopedValue 模式下 set() 须在 runWith 作用域内调用，路由切换测试强制 ThreadLocal（项目既定测试策略）
                    ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
                    DataSourceRouter router = ctx.getBean(DataSourceRouter.class);
                    assertThat(router.getAllDataSources()).hasSize(2);

                    router.setCurrentDataSource("backup");
                    assertThat(router.getDataSource().getName()).isEqualTo("backup");

                    router.clearCurrentDataSource();
                    assertThat(router.getDataSource().getName()).isEqualTo("primary");
                });
    }

    @Test
    void disabled_skipsBeans() {
        context.withPropertyValues("framework.data.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(DataSource.class);
                    assertThat(ctx).doesNotHaveBean(DataSourceRouter.class);
                });
    }

    @Test
    void mybatisType_skipsBeans() {
        context.withPropertyValues("framework.data.type=mybatis")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(DataSource.class);
                    assertThat(ctx).doesNotHaveBean(DataSourceRouter.class);
                });
    }
}