package cn.jowen.framework.plugin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginManagerTest {

    @Test
    void registerStartsAndStopsPlugin() {
        DefaultPluginManager manager = new DefaultPluginManager();
        TestPlugin plugin = new TestPlugin();
        manager.register(plugin);
        assertThat(plugin.started).isTrue();
        assertThat(manager.get("test")).isSameAs(plugin);

        manager.unregister("test");
        assertThat(plugin.stopped).isTrue();
        assertThat(manager.get("test")).isNull();
    }

    @Test
    void duplicateIdRejected() {
        DefaultPluginManager manager = new DefaultPluginManager();
        manager.register(new TestPlugin());
        assertThatThrownBy(() -> manager.register(new TestPlugin()))
                .isInstanceOf(PluginLoader.PluginException.class);
    }

    @Test
    void stopAllReversesOrder() {
        DefaultPluginManager manager = new DefaultPluginManager();
        OrderPlugin a = new OrderPlugin("a");
        OrderPlugin b = new OrderPlugin("b");
        manager.register(a);
        manager.register(b);
        manager.stopAll();
        // 逆序停止：b 先停，a 后停
        assertThat(b.stopOrder).isLessThan(a.stopOrder);
    }

    static final class TestPlugin implements Plugin {
        boolean started = false;
        boolean stopped = false;

        @Override
        public String id() {
            return "test";
        }

        @Override
        public String version() {
            return "1.0.0";
        }

        @Override
        public void afterPropertiesSet() {
            started = true;
        }

        @Override
        public void destroy() {
            stopped = true;
        }
    }

    static final class OrderPlugin implements Plugin {
        final String id;
        int stopOrder = -1;
        private static int counter = 0;

        OrderPlugin(String id) {
            this.id = id;
        }

        @Override
        public String id() {
            return id;
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
            stopOrder = ++counter;
        }
    }
}
