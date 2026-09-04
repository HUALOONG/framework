package cn.jowen.framework.plugin.api;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginDefaultMethodsTest {

    @Test
    void defaultExtensionAccessors_returnEmptyLists() {
        // 匿名实现不重写 getExtensions / getExtensionPoints，直接命中接口默认方法
        Plugin plugin = new Plugin() {
            @Override
            public void start(PluginContext context) {
            }

            @Override
            public void stop() {
            }

            @Override
            public PluginDescriptor getDescriptor() {
                return null;
            }

            @Override
            public PluginState getState() {
                return PluginState.CREATED;
            }
        };
        assertThat(plugin.getExtensions()).isEmpty();
        assertThat(plugin.getExtensionPoints()).isEmpty();
    }
}
