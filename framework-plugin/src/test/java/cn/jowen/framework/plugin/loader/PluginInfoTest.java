package cn.jowen.framework.plugin.loader;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PluginInfoTest {

    @Test
    void descriptor_returnsDescriptor() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new TestPluginClassLoader(urls, parent,
                java.util.List.of(), java.util.List.of(), java.util.List.of());
        PluginInfo info = new PluginInfo(desc, cl, null, null, Instant.now());
        assertThat(info.descriptor()).isSameAs(desc);
    }

    @Test
    void classLoader_returnsLoader() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new TestPluginClassLoader(urls, parent,
                java.util.List.of(), java.util.List.of(), java.util.List.of());
        PluginInfo info = new PluginInfo(desc, cl, null, null, Instant.now());
        assertThat(info.classLoader()).isSameAs(cl);
    }

    @Test
    void close_delegatesToClassLoader() throws java.io.IOException {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new TestPluginClassLoader(urls, parent,
                java.util.List.of(), java.util.List.of(), java.util.List.of());
        PluginInfo info = new PluginInfo(desc, cl, null, null, Instant.now());
        info.close();
    }

    @Test
    void loadedAt_isSet() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        URL[] urls = new URL[0];
        ClassLoader parent = getClass().getClassLoader();
        PluginClassLoader cl = new TestPluginClassLoader(urls, parent,
                java.util.List.of(), java.util.List.of(), java.util.List.of());
        Instant now = Instant.now();
        PluginInfo info = new PluginInfo(desc, cl, null, null, now);
        assertThat(info.loadedAt()).isEqualTo(now);
    }

    static class TestPluginClassLoader extends PluginClassLoader {
        TestPluginClassLoader(URL[] urls, ClassLoader parent,
                              java.util.List<String> exported, java.util.List<String> imported,
                              java.util.List<String> hidden) {
            super(urls, parent, exported, imported, hidden);
        }
        @Override
        protected Class<?> doLoadClass(String name, boolean resolve) throws ClassNotFoundException {
            return getParent().loadClass(name);
        }
    }
}
