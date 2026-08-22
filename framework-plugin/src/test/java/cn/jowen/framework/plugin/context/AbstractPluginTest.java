package cn.jowen.framework.plugin.context;

import cn.jowen.framework.plugin.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractPluginTest {

    @Test
    void lifecycleTransitions() {
        TestablePlugin plugin = new TestablePlugin();
        assertThat(plugin.getState()).isEqualTo(AbstractPlugin.State.CREATED);

        plugin.afterPropertiesSet();
        assertThat(plugin.getState()).isEqualTo(AbstractPlugin.State.STARTED);
        assertThat(plugin.initCalled).isTrue();
        assertThat(plugin.startCalled).isTrue();

        plugin.destroy();
        assertThat(plugin.getState()).isEqualTo(AbstractPlugin.State.STOPPED);
        assertThat(plugin.stopCalled).isTrue();
    }

    @Test
    void initFailureSetsFailedState() {
        FailingInitPlugin plugin = new FailingInitPlugin();
        org.assertj.core.api.Assertions.assertThatThrownBy(plugin::afterPropertiesSet)
                .isInstanceOf(RuntimeException.class);
        assertThat(plugin.getState()).isEqualTo(AbstractPlugin.State.FAILED);
    }

    static class TestablePlugin extends AbstractPlugin {
        boolean initCalled = false;
        boolean startCalled = false;
        boolean stopCalled = false;

        TestablePlugin() {
            super(PluginDescriptor.of("test", "1.0.0", "com.test.TestablePlugin"));
        }

        @Override
        protected void doInit() { initCalled = true; }
        @Override
        protected void doStart() { startCalled = true; }
        @Override
        protected void doStop() { stopCalled = true; }
    }

    static class FailingInitPlugin extends AbstractPlugin {
        FailingInitPlugin() {
            super(PluginDescriptor.of("fail", "1.0.0", "com.fail.Fail"));
        }
        @Override
        protected void doInit() throws Exception { throw new RuntimeException("init failed"); }
    }
}
