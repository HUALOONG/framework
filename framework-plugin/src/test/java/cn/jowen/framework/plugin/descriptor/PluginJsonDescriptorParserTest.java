package cn.jowen.framework.plugin.descriptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginJsonDescriptorParserTest {

    @Test
    void parseJson_basicDescriptor() throws PluginJsonDescriptorParser.DescriptorParseException {
        String json = """
                {
                  "pluginId": "my-plugin",
                  "version": "1.0.0",
                  "pluginClass": "com.example.MyPlugin"
                }
                """;
        PluginDescriptor desc = new PluginJsonDescriptorParser().parseJson(json);
        assertThat(desc.pluginId()).isEqualTo("my-plugin");
        assertThat(desc.version()).isEqualTo("1.0.0");
        assertThat(desc.pluginClass()).isEqualTo("com.example.MyPlugin");
    }

    @Test
    void parseJson_withOptionalFields() throws PluginJsonDescriptorParser.DescriptorParseException {
        String json = """
                {
                  "pluginId": "my-plugin",
                  "pluginName": "My Plugin",
                  "version": "2.0.0",
                  "description": "A test plugin",
                  "author": "dev",
                  "license": "MIT",
                  "pluginClass": "com.example.MyPlugin",
                  "enabledByDefault": true
                }
                """;
        PluginDescriptor desc = new PluginJsonDescriptorParser().parseJson(json);
        assertThat(desc.pluginName()).isEqualTo("My Plugin");
        assertThat(desc.description()).isEqualTo("A test plugin");
        assertThat(desc.author()).isEqualTo("dev");
        assertThat(desc.license()).isEqualTo("MIT");
        assertThat(desc.enabledByDefault()).isTrue();
    }

    @Test
    void parseJson_legacyIdField() throws PluginJsonDescriptorParser.DescriptorParseException {
        String json = """
                {
                  "id": "legacy-plugin",
                  "version": "1.0.0",
                  "class": "com.example.Plugin"
                }
                """;
        PluginDescriptor desc = new PluginJsonDescriptorParser().parseJson(json);
        assertThat(desc.pluginId()).isEqualTo("legacy-plugin");
        assertThat(desc.pluginClass()).isEqualTo("com.example.Plugin");
    }

    @Test
    void parseJson_withRequiresStringArray() throws PluginJsonDescriptorParser.DescriptorParseException {
        String json = """
                {
                  "pluginId": "A",
                  "version": "1.0",
                  "pluginClass": "com.A",
                  "requires": ["B", "C"]
                }
                """;
        PluginDescriptor desc = new PluginJsonDescriptorParser().parseJson(json);
        assertThat(desc.requires()).hasSize(2);
        assertThat(desc.requires().getFirst().pluginId()).isEqualTo("B");
    }

    @Test
    void parseJson_withRequiresObjectArray() throws PluginJsonDescriptorParser.DescriptorParseException {
        String json = """
                {
                  "pluginId": "A",
                  "version": "1.0",
                  "pluginClass": "com.A",
                  "requires": [
                    { "pluginId": "B", "versionRange": "[1.0.0,3.0.0)", "optional": false },
                    { "pluginId": "C", "versionRange": "[2.0.0,9.0.0)" }
                  ]
                }
                """;
        PluginDescriptor desc = new PluginJsonDescriptorParser().parseJson(json);
        assertThat(desc.requires()).hasSize(2);
        assertThat(desc.requires().getFirst().pluginId()).isEqualTo("B");
        assertThat(desc.requires().getFirst().versionRange()).isNotNull();
        assertThat(desc.requires().get(1).pluginId()).isEqualTo("C");
    }

    @Test
    void parseJson_stringValueWithBracketsAndCommas() throws PluginJsonDescriptorParser.DescriptorParseException {
        String json = """
                {
                  "pluginId": "A",
                  "version": "1.0",
                  "pluginClass": "com.A",
                  "description": "Range [1.0,2.0), list {a,b}, ok"
                }
                """;
        PluginDescriptor desc = new PluginJsonDescriptorParser().parseJson(json);
        assertThat(desc.description()).isEqualTo("Range [1.0,2.0), list {a,b}, ok");
    }

    @Test
    void parseJson_missingPluginId_throws() {
        String json = """
                {
                  "version": "1.0.0",
                  "pluginClass": "com.example.Plugin"
                }
                """;
        assertThatThrownBy(() -> new PluginJsonDescriptorParser().parseJson(json))
                .isInstanceOf(PluginJsonDescriptorParser.DescriptorParseException.class)
                .hasMessageContaining("缺少必需字段");
    }

    @Test
    void parseJson_missingVersion_throws() {
        String json = """
                {
                  "pluginId": "p",
                  "pluginClass": "com.example.Plugin"
                }
                """;
        assertThatThrownBy(() -> new PluginJsonDescriptorParser().parseJson(json))
                .isInstanceOf(PluginJsonDescriptorParser.DescriptorParseException.class);
    }

    @Test
    void parseJson_invalidJson_throws() {
        assertThatThrownBy(() -> new PluginJsonDescriptorParser().parseJson("not json"))
                .isInstanceOf(PluginJsonDescriptorParser.DescriptorParseException.class);
    }

    @Test
    void supportedExtensions_returnsJson() {
        assertThat(new PluginJsonDescriptorParser().supportedExtensions()).containsExactly("json");
    }
}
