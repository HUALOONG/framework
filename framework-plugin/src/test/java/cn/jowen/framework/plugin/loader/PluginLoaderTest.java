package cn.jowen.framework.plugin.loader;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginLoaderTest {

    @Test
    void descriptor_returnsDescriptor() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        PluginLoader loader = new PluginLoader(desc, new URL[0], getClass().getClassLoader());
        assertThat(loader.descriptor()).isSameAs(desc);
    }

    @Test
    void descriptor_withCustomStrategy() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        PluginLoader loader = new PluginLoader(desc, new URL[0],
                getClass().getClassLoader(), ClassLoadingStrategy.PARENT_FIRST);
        assertThat(loader.descriptor()).isSameAs(desc);
    }

    @Test
    void getClassLoader_returnsClassLoader() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        PluginLoader loader = new PluginLoader(desc, new URL[0], getClass().getClassLoader());
        assertThat(loader.getClassLoader()).isInstanceOf(PluginClassLoader.class);
    }

    @Test
    void getPluginPath_null_whenConstructedWithURLs() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        PluginLoader loader = new PluginLoader(desc, new URL[0], getClass().getClassLoader());
        assertThat(loader.getPluginPath()).isNull();
    }

    @Test
    void load_invalidPluginClass_throwsPluginLoadException() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.NonExistent");
        PluginLoader loader = new PluginLoader(desc, new URL[0], getClass().getClassLoader());
        assertThatThrownBy(loader::load)
                .isInstanceOf(PluginLoader.PluginLoadException.class);
    }

    @Test
    void load_pluginDoesNotImplementPlugin_throwsPluginLoadException() {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "java.lang.String");
        PluginLoader loader = new PluginLoader(desc, new URL[0], getClass().getClassLoader());
        assertThatThrownBy(loader::load)
                .isInstanceOf(PluginLoader.PluginLoadException.class)
                .hasMessageContaining("未实现 Plugin");
    }

    @Test
    void close_doesNotThrow() throws java.io.IOException {
        PluginDescriptor desc = PluginDescriptor.of("test", "1.0", "com.example.Test");
        PluginLoader loader = new PluginLoader(desc, new URL[0], getClass().getClassLoader());
        loader.close();
    }
}
