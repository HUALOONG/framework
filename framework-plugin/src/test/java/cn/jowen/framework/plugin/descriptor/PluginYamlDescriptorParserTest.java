package cn.jowen.framework.plugin.descriptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PluginYamlDescriptorParser} 单元测试：YAML 描述符解析（与 JSON 字段语义一致）。
 *
 * @author 王飞
 * @since 2026-08-27
 */
class PluginYamlDescriptorParserTest {

    @Test
    void parseYaml_basicDescriptor() {
        String yaml = """
                pluginId: my-plugin
                version: 1.0.0
                pluginClass: com.example.MyPlugin
                """;
        PluginDescriptor desc = new PluginYamlDescriptorParser().parseYaml(yaml);
        assertThat(desc.pluginId()).isEqualTo("my-plugin");
        assertThat(desc.version()).isEqualTo("1.0.0");
        assertThat(desc.pluginClass()).isEqualTo("com.example.MyPlugin");
    }

    @Test
    void parseYaml_withRequiresAndExtensions() {
        String yaml = """
                pluginId: my-plugin
                version: 2.0.0
                pluginClass: com.example.MyPlugin
                requires:
                  - pluginId: base-plugin
                    versionRange: "[1.0.0,3.0.0)"
                extensions:
                  - point: com.example.ExtensionPoint
                    impl: com.example.MyExtension
                """;
        PluginDescriptor desc = new PluginYamlDescriptorParser().parseYaml(yaml);
        assertThat(desc.requires()).hasSize(1);
        assertThat(desc.requires().getFirst().pluginId()).isEqualTo("base-plugin");
        assertThat(desc.extensions()).isNotEmpty();
    }

    @Test
    void parseYaml_legacyFieldNames() {
        String yaml = """
                id: legacy-plugin
                version: 1.0.0
                class: com.example.Plugin
                """;
        PluginDescriptor desc = new PluginYamlDescriptorParser().parseYaml(yaml);
        assertThat(desc.pluginId()).isEqualTo("legacy-plugin");
        assertThat(desc.pluginClass()).isEqualTo("com.example.Plugin");
    }

    @Test
    void parseYaml_nonMapRoot_throws() {
        assertThatThrownBy(() -> new PluginYamlDescriptorParser().parseYaml("- a\n- b"))
                .isInstanceOf(PluginJsonDescriptorParser.DescriptorParseException.class);
    }

    @Test
    void supportedExtensions_includesYaml() {
        assertThat(new PluginYamlDescriptorParser().supportedExtensions())
                .contains("yaml", "yml");
    }
}