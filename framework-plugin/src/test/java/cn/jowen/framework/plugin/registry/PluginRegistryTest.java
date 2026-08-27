package cn.jowen.framework.plugin.registry;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginRegistryTest {

    private PluginRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new PluginRegistry();
    }

    @Test
    void register_andGetPlugin() {
        TestPlugin plugin = new TestPlugin("p1");
        registry.register(plugin);
        assertThat(registry.getPlugin("p1")).isSameAs(plugin);
    }

    @Test
    void register_duplicateId_throwsIllegalState() {
        registry.register(new TestPlugin("p1"));
        assertThatThrownBy(() -> registry.register(new TestPlugin("p1")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已存在");
    }

    @Test
    void register_null_throwsIllegalArgument() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unregister_removesPlugin() {
        registry.register(new TestPlugin("p1"));
        Plugin removed = registry.unregister("p1");
        assertThat(removed).isNotNull();
        assertThat(registry.getPlugin("p1")).isNull();
    }

    @Test
    void unregister_nonExistent_returnsNull() {
        assertThat(registry.unregister("missing")).isNull();
    }

    @Test
    void getPlugins_returnsAll() {
        registry.register(new TestPlugin("p1"));
        registry.register(new TestPlugin("p2"));
        assertThat(registry.getPlugins()).hasSize(2);
    }

    @Test
    void getPluginsByState_filters() {
        registry.register(new TestPlugin("p1"));
        registry.register(new RunningPlugin("p2"));
        assertThat(registry.getPluginsByState(PluginState.STARTED)).hasSize(1);
        assertThat(registry.getPluginsByState(PluginState.CREATED)).hasSize(1);
    }

    @Test
    void size_returnsCount() {
        registry.register(new TestPlugin("p1"));
        registry.register(new TestPlugin("p2"));
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    void clear_removesAll() {
        registry.register(new TestPlugin("p1"));
        registry.clear();
        assertThat(registry.size()).isEqualTo(0);
    }

    static class TestPlugin implements Plugin {
        private final String id;
        public TestPlugin(String id) { this.id = id; }
        public PluginDescriptor getDescriptor() { return new PluginDescriptor(id); }
        public PluginState getState() { return PluginState.CREATED; }
        public void start(PluginContext ctx) {}
        public void stop() {}
    }

    static class RunningPlugin implements Plugin {
        private final String id;
        public RunningPlugin(String id) { this.id = id; }
        public PluginDescriptor getDescriptor() { return new PluginDescriptor(id); }
        public PluginState getState() { return PluginState.STARTED; }
        public void start(PluginContext ctx) {}
        public void stop() {}
    }
}
