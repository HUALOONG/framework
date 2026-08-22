package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.core.spi.Activate;
import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 插件装配集成测试：验证 {@link PluginManager} 自动装配，且 classpath 中带 {@code @Activate} 的插件被自动注册。
 */
public class PluginAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class));

    @Test
    void pluginManagerRegistered() {
        runner.run(context -> assertThat(context).hasSingleBean(PluginManager.class));
    }

    @Test
    void activatePluginAutoRegistered() {
        runner.run(context -> {
            PluginManager manager = context.getBean(PluginManager.class);
            assertThat(manager.get("activated")).isNotNull();
        });
    }

    @Test
    void pluginDisabledByProperty() {
        runner.withPropertyValues("framework.plugin.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(PluginManager.class));
    }

    /** 经 SPI 文件注册、带 {@code @Activate} 的示例插件，用于验证自动注册。 */
    @Activate
    public static final class ActivatedPlugin implements Plugin {

        public ActivatedPlugin() {
        }

        @Override
        public String id() {
            return "activated";
        }

        @Override
        public String version() {
            return "1.0.0";
        }

        @Override
        public void afterPropertiesSet() {
            // 无需初始化
        }

        @Override
        public void destroy() {
            // 无需清理
        }
    }
}
