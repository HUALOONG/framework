package cn.jowen.framework.plugin.support;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.context.SharedData;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AbstractPluginTest {

    private TestAbstractPlugin plugin;

    @BeforeEach
    void setUp() {
        PluginDescriptor desc = new PluginDescriptor("test-plugin");
        plugin = new TestAbstractPlugin(desc);
    }

    @Test
    void getDescriptor_returnsDescriptor() {
        assertThat(plugin.getDescriptor().pluginId()).isEqualTo("test-plugin");
    }

    @Test
    void getState_initialCreated() {
        assertThat(plugin.getState()).isEqualTo(PluginState.CREATED);
    }

    @Test
    void start_success_transitionsToStarted() {
        TestContext ctx = new TestContext();
        plugin.start(ctx);
        assertThat(plugin.getState()).isEqualTo(PluginState.STARTED);
        assertThat(plugin.wasStarted()).isTrue();
    }

    @Test
    void start_failure_transitionsToFailed() {
        plugin = new TestAbstractPlugin(new PluginDescriptor("test-plugin"));
        plugin.failOnStart(true);
        TestContext ctx = new TestContext();
        assertThatThrownBy(() -> plugin.start(ctx))
                .isInstanceOf(RuntimeException.class);
        assertThat(plugin.getState()).isEqualTo(PluginState.FAILED);
    }

    @Test
    void stop_success_transitionsToStopped() {
        plugin.start(new TestContext());
        plugin.stop();
        assertThat(plugin.getState()).isEqualTo(PluginState.STOPPED);
        assertThat(plugin.wasStopped()).isTrue();
    }

    @Test
    void stop_failure_transitionsToFailed() {
        plugin.failOnStop(true);
        assertThatThrownBy(() -> plugin.stop())
                .isInstanceOf(RuntimeException.class);
        assertThat(plugin.getState()).isEqualTo(PluginState.FAILED);
    }

    @Test
    void getAndSetContext() {
        TestContext ctx = new TestContext();
        plugin.setContext(ctx);
        assertThat(plugin.getContext()).isSameAs(ctx);
    }

    @Test
    void doInit_calledBeforeStart() {
        assertThat(plugin.initOrder()).isEmpty();
        plugin.start(new TestContext());
        assertThat(plugin.initOrder()).contains("doInit");
    }

    static class TestAbstractPlugin extends AbstractPlugin {
        private boolean started = false;
        private boolean stopped = false;
        private boolean failStart = false;
        private boolean failStop = false;
        private final java.util.List<String> initOrder = new java.util.ArrayList<>();

        TestAbstractPlugin(PluginDescriptor descriptor) {
            super(descriptor);
        }

        void failOnStart(boolean fail) { this.failStart = fail; }
        void failOnStop(boolean fail) { this.failStop = fail; }
        boolean wasStarted() { return started; }
        boolean wasStopped() { return stopped; }
        java.util.List<String> initOrder() { return initOrder; }

        @Override
        protected void doInit() { initOrder.add("doInit"); }

        @Override
        protected void doStart(PluginContext context) {
            started = true;
            if (failStart) throw new RuntimeException("start failed");
        }

        @Override
        protected void doStop() {
            stopped = true;
            if (failStop) throw new RuntimeException("stop failed");
        }
    }

    static class TestContext implements PluginContext {
        public String getPluginId() { return "test"; }
        public PluginDescriptor getPluginDescriptor() { return new PluginDescriptor("test"); }
        public cn.jowen.framework.plugin.context.PluginConfiguration getConfiguration() { return null; }
        public SharedData getSharedData() { return null; }
        public PluginManager getPluginManager() { return null; }
        public ClassLoader getApplicationClassLoader() { return getClass().getClassLoader(); }
        public ClassLoader getPluginClassLoader() { return getClass().getClassLoader(); }
        public Object getSpringContext() { return null; }
        public void publishEvent(cn.jowen.framework.plugin.event.PluginEvent event) {}
        public void close() {}
    }
}
