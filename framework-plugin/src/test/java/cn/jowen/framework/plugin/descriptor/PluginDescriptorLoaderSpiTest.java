package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.core.spi.ExtensionLoader;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PluginDescriptorLoader} 统一 SPI 化测试：解析器经 META-INF/services 与注解被 core ExtensionLoader 发现。
 */
class PluginDescriptorLoaderSpiTest {

    @Test
    void parsers_discoveredViaExtensionLoader() {
        ExtensionLoader<PluginDescriptorLoader> loader =
                ExtensionLoader.getExtensionLoader(PluginDescriptorLoader.class);
        assertThat(loader.getAllExtensions())
                .extracting(parser -> parser.getClass().getSimpleName())
                .containsExactlyInAnyOrder("PluginJsonDescriptorParser", "PluginYamlDescriptorParser");
    }

    @Test
    void defaultExtension_isJsonParser() {
        ExtensionLoader<PluginDescriptorLoader> loader =
                ExtensionLoader.getExtensionLoader(PluginDescriptorLoader.class);
        assertThat(loader.getDefaultExtension()).isInstanceOf(PluginJsonDescriptorParser.class);
    }

    @Test
    void namedLookup_resolvesBothParsers() {
        ExtensionLoader<PluginDescriptorLoader> loader =
                ExtensionLoader.getExtensionLoader(PluginDescriptorLoader.class);
        assertThat(loader.getExtension("json")).isInstanceOf(PluginJsonDescriptorParser.class);
        assertThat(loader.getExtension("yaml")).isInstanceOf(PluginYamlDescriptorParser.class);
    }
}