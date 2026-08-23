package cn.jowen.framework.plugin;

import cn.jowen.framework.plugin.config.PluginApplicationContext;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginManagerSpringContextTest {

    @Test
    void springContextCreatedForSpringEnabledPlugin() {
        var parent = new AnnotationConfigApplicationContext();
        parent.refresh();
        try {
            PluginApplicationContext ctx = PluginApplicationContext.create(parent);
            ctx.configure("", false);

            DefaultPluginManager manager = new DefaultPluginManager();
            manager.setSpringContext(ctx);

            PluginDescriptor desc = new PluginDescriptor("spring-plugin", "1.0.0",
                    PluginManagerSpringContextTest.class.getName() + "$SpringPlugin",
                    "", List.of(), List.of(), true);

            manager.load(new PluginLoader(desc, new URL[0],
                    PluginManagerSpringContextTest.class.getClassLoader()));

            assertThat(manager.get("spring-plugin")).isNotNull();
            assertThat(ctx.hasChild("spring-plugin")).isTrue();

            manager.unregister("spring-plugin");
            assertThat(ctx.hasChild("spring-plugin")).isFalse();
        } finally {
            parent.close();
        }
    }

    @Test
    void noSpringContextWhenDisabled() {
        var parent = new AnnotationConfigApplicationContext();
        parent.refresh();
        try {
            PluginApplicationContext ctx = PluginApplicationContext.create(parent);
            ctx.configure("", false);

            DefaultPluginManager manager = new DefaultPluginManager();
            manager.setSpringContext(ctx);

            PluginDescriptor desc = new PluginDescriptor("normal-plugin", "1.0.0",
                    PluginManagerSpringContextTest.class.getName() + "$NormalPlugin",
                    "", List.of(), List.of(), false);

            manager.load(new PluginLoader(desc, new URL[0],
                    PluginManagerSpringContextTest.class.getClassLoader()));

            assertThat(manager.get("normal-plugin")).isNotNull();
            // springEnabled=false，不应创建子容器
            assertThat(ctx.hasChild("normal-plugin")).isFalse();
        } finally {
            parent.close();
        }
    }

    @Test
    void noSpringContextWhenNull() {
        DefaultPluginManager manager = new DefaultPluginManager();
        // springContext 为 null（默认）

        PluginDescriptor desc = new PluginDescriptor("no-spring", "1.0.0",
                PluginManagerSpringContextTest.class.getName() + "$DummyPlugin",
                "", List.of(), List.of(), true);

        manager.load(new PluginLoader(desc, new URL[0],
                PluginManagerSpringContextTest.class.getClassLoader()));

        assertThat(manager.get("no-spring")).isNotNull();
        // springContext 为 null，即使 springEnabled=true 也不应抛异常
    }

    public static class SpringPlugin implements Plugin {
        @Override public String id() { return "spring-plugin"; }
        @Override public String version() { return "1.0.0"; }
        @Override public PluginDescriptor descriptor() {
            return new PluginDescriptor("spring-plugin", "1.0.0",
                    SpringPlugin.class.getName(), "", java.util.List.of(), java.util.List.of(), true);
        }
        @Override public void afterPropertiesSet() {}
        @Override public void destroy() {}
    }

    public static class NormalPlugin implements Plugin {
        @Override public String id() { return "normal-plugin"; }
        @Override public String version() { return "1.0.0"; }
        @Override public PluginDescriptor descriptor() {
            return new PluginDescriptor("normal-plugin", "1.0.0",
                    NormalPlugin.class.getName(), "", java.util.List.of(), java.util.List.of(), false);
        }
        @Override public void afterPropertiesSet() {}
        @Override public void destroy() {}
    }

    public static class DummyPlugin implements Plugin {
        @Override public String id() { return "no-spring"; }
        @Override public String version() { return "1.0.0"; }
        @Override public PluginDescriptor descriptor() {
            return new PluginDescriptor("no-spring", "1.0.0",
                    DummyPlugin.class.getName(), "", java.util.List.of(), java.util.List.of(), true);
        }
        @Override public void afterPropertiesSet() {}
        @Override public void destroy() {}
    }
}
