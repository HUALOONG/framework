package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link ExtensionFactory} 测试。
 */
class ExtensionFactoryTest {

    @Test
    void getRegistry_and_getExtensions() {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginContext ctx = mock(PluginContext.class);
        ExtensionFactory factory = new ExtensionFactory(registry, ctx);

        assertThat(factory.getRegistry()).isSameAs(registry);
        assertThat(factory.getExtensions("no.such.point")).isEmpty();
    }

    @Test
    void loadFromDefinitions_empty_isNoOp() {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginContext ctx = mock(PluginContext.class);
        ExtensionFactory factory = new ExtensionFactory(registry, ctx);
        factory.loadFromDefinitions(List.of(), getClass().getClassLoader());
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void loadFromPackage_directoryProtocol_registersExtensions() {
        ExtensionRegistry registry = new ExtensionRegistry();
        PluginContext ctx = mock(PluginContext.class);
        ExtensionFactory factory = new ExtensionFactory(registry, ctx);
        factory.loadFromPackage(getClass().getClassLoader(),
                "cn.jowen.framework.plugin.extension.dirscan");
        assertThat(registry.getExtensions("dir.point")).hasSize(2);
    }
}
