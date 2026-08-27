package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.context.SharedData;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.event.PluginEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginLifecycleManagerTest {

    private PluginLifecycleManager manager;
    private final List<PluginEvent> capturedEvents = new ArrayList<>();

    @BeforeEach
    void setUp() {
        manager = new PluginLifecycleManager();
        manager.addListener(capturedEvents::add);
        capturedEvents.clear();
    }

    @Test
    void initialize_setsStateToStarting() {
        TestPlugin plugin = new TestPlugin("p1");
        TestContext ctx = new TestContext();
        manager.initialize("p1", plugin, ctx);
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STARTING);
        assertThat(capturedEvents).hasSize(1);
        assertThat(capturedEvents.get(0)).isInstanceOf(cn.jowen.framework.plugin.event.PluginStartingEvent.class);
    }

    @Test
    void start_transitionsToStarted() {
        TestPlugin plugin = new TestPlugin("p1");
        manager.initialize("p1", plugin, new TestContext());
        manager.start("p1");
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STARTED);
    }

    @Test
    void start_nonExistentPlugin_noOp() {
        manager.start("missing");
        assertThat(manager.getPlugin("missing")).isNull();
    }

    @Test
    void stop_transitionsToStopped() {
        TestPlugin plugin = new TestPlugin("p1");
        manager.initialize("p1", plugin, new TestContext());
        manager.start("p1");
        manager.stop("p1");
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STOPPED);
    }

    @Test
    void restart_stopsAndStarts() {
        TestPlugin plugin = new TestPlugin("p1");
        manager.initialize("p1", plugin, new TestContext());
        manager.start("p1");
        manager.restart("p1");
        assertThat(manager.getState("p1")).isEqualTo(PluginState.STARTED);
    }

    @Test
    void destroy_removesPlugin() {
        TestPlugin plugin = new TestPlugin("p1");
        manager.initialize("p1", plugin, new TestContext());
        manager.start("p1");
        manager.destroy("p1");
        assertThat(manager.getPlugin("p1")).isNull();
        assertThat(manager.getState("p1")).isEqualTo(PluginState.CREATED);
    }


    @Test
    void getPlugin_returnsRegisteredPlugin() {
        TestPlugin plugin = new TestPlugin("p1");
        manager.initialize("p1", plugin, new TestContext());
        assertThat(manager.getPlugin("p1")).isSameAs(plugin);
    }

    @Test
    void getPlugins_returnsAllRegistered() {
        manager.initialize("p1", new TestPlugin("p1"), new TestContext());
        manager.initialize("p2", new TestPlugin("p2"), new TestContext());
        assertThat(manager.getPlugins()).hasSize(2);
    }

    @Test
    void getPluginsByState_filtersCorrectly() {
        TestPlugin plugin = new TestPlugin("p1");
        manager.initialize("p1", plugin, new TestContext());
        manager.start("p1");
        assertThat(manager.getPluginsByState(PluginState.STARTED)).hasSize(1);
        assertThat(manager.getPluginsByState(PluginState.STOPPED)).isEmpty();
    }

    static class TestPlugin implements Plugin {
        private final String id;
        private PluginContext ctx;

        TestPlugin(String id) { this.id = id; }

        public void start(PluginContext context) { this.ctx = context; }
        public void stop() {}
        public PluginDescriptor getDescriptor() { return new PluginDescriptor(id); }
        public PluginState getState() { return PluginState.STARTED; }
    }

    static class TestContext implements PluginContext {
        public String getPluginId() { return "p1"; }
        public PluginDescriptor getPluginDescriptor() { return new PluginDescriptor("p1"); }
        public cn.jowen.framework.plugin.context.PluginConfiguration getConfiguration() { return null; }
        public SharedData getSharedData() { return null; }
        public cn.jowen.framework.plugin.api.PluginManager getPluginManager() { return null; }
        public ClassLoader getApplicationClassLoader() { return getClass().getClassLoader(); }
        public ClassLoader getPluginClassLoader() { return getClass().getClassLoader(); }
        public Object getSpringContext() { return null; }
        public void publishEvent(PluginEvent event) {}
        public void close() {}
    }
}
