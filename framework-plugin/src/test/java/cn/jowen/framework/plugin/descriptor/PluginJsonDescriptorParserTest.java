package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.plugin.PluginDescriptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginJsonDescriptorParserTest {

    private final PluginJsonDescriptorParser parser = new PluginJsonDescriptorParser();

    @Test
    void parseFullDescriptor() {
        String json = "{\"id\": \"test-plugin\", \"version\": \"1.2.3\", \"class\": \"com.example.TestPlugin\","
                + "\"description\": \"测试插件\", \"dependencies\": [\"dep-a\", \"dep-b\"],"
                + "\"exportedPackages\": [\"com.example.api\"], \"springEnabled\": true}";
        PluginDescriptor desc = parser.parseJson(json);
        assertThat(desc.id()).isEqualTo("test-plugin");
        assertThat(desc.version()).isEqualTo("1.2.3");
        assertThat(desc.className()).isEqualTo("com.example.TestPlugin");
        assertThat(desc.description()).isEqualTo("测试插件");
        assertThat(desc.dependencies()).containsExactly("dep-a", "dep-b");
    }

    @Test
    void parseMinimalDescriptor() {
        String json = "{\"id\": \"a\", \"version\": \"1.0.0\", \"class\": \"com.a.A\"}";
        PluginDescriptor desc = parser.parseJson(json);
        assertThat(desc.id()).isEqualTo("a");
        assertThat(desc.dependencies()).isEmpty();
        assertThat(desc.springEnabled()).isFalse();
    }

    @Test
    void parseMissingRequiredField() {
        String json = "{\"id\": \"x\"}";
        assertThatThrownBy(() -> parser.parseJson(json))
                .isInstanceOf(PluginJsonDescriptorParser.DescriptorParseException.class);
    }

    @Test
    void parseInvalidJson() {
        assertThatThrownBy(() -> parser.parseJson("not-json"))
                .isInstanceOf(PluginJsonDescriptorParser.DescriptorParseException.class);
    }

    @Test
    void parseWithArray() {
        String json = "{\"id\": \"p\", \"version\": \"2.0.0\", \"class\": \"com.p.P\","
                + "\"dependencies\": [\"d1\"], \"exportedPackages\": [\"com.p.api\", \"com.p.internal\"]}";
        PluginDescriptor desc = parser.parseJson(json);
        assertThat(desc.dependencies()).containsExactly("d1");
    }
}
