package cn.jowen.framework.boot.autoconfigure.observability;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 可观测性装配集成测试：验证插件信息端点注册并能读取 SPI 注册的插件。
 */
class ObservabilityAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class));

    @Test
    void pluginEndpointRegistered() {
        runner.run(context -> assertThat(context).hasSingleBean(PluginEndpoint.class));
    }

    @Test
    void endpointListsActivatedPlugins() {
        runner.run(context -> {
            PluginEndpoint endpoint = context.getBean(PluginEndpoint.class);
            assertThat(endpoint.plugins()).extracting(PluginEndpoint.PluginInfo::id)
                    .contains("activated");
        });
    }

    @Test
    void endpointReadsSinglePlugin() {
        runner.run(context -> {
            PluginEndpoint endpoint = context.getBean(PluginEndpoint.class);
            assertThat(endpoint.plugin("activated")).isNotNull();
            assertThat(endpoint.plugin("missing")).isNull();
        });
    }
}
