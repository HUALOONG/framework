package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.spi.PluginSpiBridge;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PluginAutoConfiguration} 装配测试：默认上下文注册桥接门面与引导器。
 */
class PluginAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(PluginAutoConfiguration.class));

    @Test
    void defaultContext_registersSpiBridgeAndBootstrap() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(PluginSpiBridge.class);
            assertThat(ctx).hasSingleBean(PluginBootstrap.class);
        });
    }
}