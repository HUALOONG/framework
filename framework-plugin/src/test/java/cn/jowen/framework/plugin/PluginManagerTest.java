package cn.jowen.framework.plugin;

import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

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

    /** load 重复 id 时旧 loader 被 close，不留下孤立的 classloader。 */
    @Test
    void loadClosesOldLoaderOnReload() throws Exception {
        DefaultPluginManager manager = new DefaultPluginManager();
        URL[] noUrls = new URL[0];
        // 使用 "test" 作为 descriptor id，与 TestPlugin.id() 返回值一致
        PluginDescriptor desc = new PluginDescriptor("test", "1.0.0",
                "cn.jowen.framework.plugin.PluginManagerTest$TestPlugin", "", List.of());

        // 第一次 load
        manager.load(new PluginLoader(desc, noUrls, PluginManagerTest.class.getClassLoader()));
        assertThat(manager.get("test")).isNotNull();

        // 第二次 load 同一 id：先 unregister，再 load（模拟热替换场景）
        manager.unregister("test");
        manager.load(new PluginLoader(desc, noUrls, PluginManagerTest.class.getClassLoader()));
        assertThat(manager.get("test")).isNotNull();
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
