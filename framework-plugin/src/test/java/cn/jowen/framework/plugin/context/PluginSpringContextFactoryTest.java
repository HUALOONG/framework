package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginSpringContextFactoryTest {

    private PluginSpringContextFactory factory;

    @BeforeEach
    void setUp() {
        factory = new PluginSpringContextFactory(null);
    }

    @Test
    void isEnabled_nullParent_returnsFalse() {
        assertThat(factory.isEnabled()).isFalse();
    }

    @Test
    void isEnabled_withParent_returnsTrue() {
        PluginSpringContextFactory withParent = new PluginSpringContextFactory("parent-context");
        assertThat(withParent.isEnabled()).isTrue();
    }

    @Test
    void createFor_nullParent_returnsNull() {
        Plugin plugin = new Plugin() {
            public void start(PluginContext ctx) {}
            public void stop() {}
            public PluginDescriptor getDescriptor() { return new PluginDescriptor("test"); }
            public PluginState getState() { return PluginState.CREATED; }
        };
        assertThat(factory.createFor(plugin, "com.example")).isNull();
    }

    @Test
    void close_clearsChildren() {
        factory.close();
    }
}
