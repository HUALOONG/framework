package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.plugin.descriptor.PluginJsonDescriptorParser.DescriptorParseException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PluginJsonDescriptorParserTest {

    private final PluginJsonDescriptorParser parser = new PluginJsonDescriptorParser();

    @Test
    void parseJson_newFormat_fullDescriptor() {
        String json = """
                {
                  "pluginId": "demo",
                  "pluginName": "Demo Plugin",
                  "version": "1.2.3",
                  "description": "demo desc",
                  "author": "me",
                  "license": "MIT",
                  "pluginClass": "com.demo.DemoPlugin",
                  "enabledByDefault": true,
                  "requires": ["base", {"pluginId": "db", "versionRange": "[1.0,2.0)"}],
                  "optionalRequires": ["opt"],
                  "provides": ["cap1", "cap2"],
                  "extensionPoints": [{"id": "ep1", "interfaceName": "com.demo.Ep", "singleton": true}],
                  "extensions": [{"id": "ex1", "extensionPointId": "ep1", "className": "com.demo.Ex", "order": 5}],
                  "configuration": {"items": [{"key": "k1", "type": "string", "defaultValue": "d", "required": true, "description": "desc"}]}
                }
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("demo");
        assertThat(d.pluginName()).isEqualTo("Demo Plugin");
        assertThat(d.version()).isEqualTo("1.2.3");
        assertThat(d.pluginClass()).isEqualTo("com.demo.DemoPlugin");
        assertThat(d.enabledByDefault()).isTrue();
        assertThat(d.requires()).hasSize(2);
        assertThat(d.requires().get(0).pluginId()).isEqualTo("base");
        assertThat(d.requires().get(1).pluginId()).isEqualTo("db");
        assertThat(d.optionalRequires()).hasSize(1);
        assertThat(d.provides()).containsExactly("cap1", "cap2");
        assertThat(d.extensionPoints()).hasSize(1);
        assertThat(d.extensionPoints().get(0).id()).isEqualTo("ep1");
        assertThat(d.extensionPoints().get(0).singleton()).isTrue();
        assertThat(d.extensions()).hasSize(1);
        assertThat(d.extensions().get(0).id()).isEqualTo("ex1");
        assertThat(d.extensions().get(0).order()).isEqualTo(5);
        assertThat(d.configuration()).isNotNull();
        assertThat(d.configuration().key()).isEqualTo("k1");
        assertThat(d.configuration().required()).isTrue();
    }

    @Test
    void parseJson_oldFormat_compatible() {
        String json = """
                {"id": "legacy", "class": "com.legacy.Legacy", "version": "0.9.0"}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("legacy");
        assertThat(d.pluginName()).isEqualTo("legacy");
        assertThat(d.pluginClass()).isEqualTo("com.legacy.Legacy");
        assertThat(d.configuration()).isNull();
        assertThat(d.extensionPoints()).isEmpty();
    }

    @Test
    void parseJson_disabledByDefault_false() {
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "enabledByDefault": false}
                """;
        assertThat(parser.parseJson(json).enabledByDefault()).isFalse();
    }

    @Test
    void parseJson_missingRequiredField_throws() {
        String json = """
                {"version": "1.0", "pluginClass": "c.X"}
                """;
        assertThatThrownBy(() -> parser.parseJson(json))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("缺少必需字段");
    }

    @Test
    void parseJson_invalidObject_throws() {
        assertThatThrownBy(() -> parser.parseJson("not a json"))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("无效的 JSON 对象");
    }

    @Test
    void parseJson_numericAndBooleanScalars() {
        // 标量解析分支：数字（long/double）、布尔、null
        String json = """
                {"pluginId": "s", "version": "1.0", "pluginClass": "c.S", "max": 42, "ratio": 3.14, "flag": true, "nothing": null}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("s");
    }

    @Test
    void load_fromJar_readsPluginJson() throws Exception {
        String json = """
                {"pluginId": "jarred", "version": "2.0.0", "pluginClass": "c.Jar"}
                """;
        Path jar = Files.createTempFile("plugin", ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             ByteArrayInputStream in = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.json"));
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) jos.write(buf, 0, n);
            jos.closeEntry();
        }
        PluginDescriptor d = parser.load(jar);
        assertThat(d.pluginId()).isEqualTo("jarred");
        assertThat(d.version()).isEqualTo("2.0.0");
        Files.deleteIfExists(jar);
    }

    @Test
    void load_fromJar_missingPluginJson_throws() throws Exception {
        // JAR 中不存在 META-INF/plugin/plugin.json -> readJarResource 抛 DescriptorParseException
        Path jar = Files.createTempFile("plugin-empty", ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar))) {
            jos.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
            jos.write("Manifest-Version: 1.0\n".getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }
        try {
            assertThatThrownBy(() -> parser.load(jar))
                    .isInstanceOf(DescriptorParseException.class)
                    .hasMessageContaining("JAR 中不存在 META-INF/plugin/plugin.json");
        } finally {
            Files.deleteIfExists(jar);
        }
    }

    @Test
    void parseJson_unquotedKey_throws() {
        // 缺少 key 起始引号（skipQuotes 返回 -1）
        assertThatThrownBy(() -> parser.parseJson("{ pluginId : \"x\" }"))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("缺少 key 起始引号");
    }

    @Test
    void parseJson_unclosedKey_throws() {
        // key 起始引号后无闭合引号（findClosingQuote 返回 -1）
        assertThatThrownBy(() -> parser.parseJson("{\"foo}"))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("key 未闭合");
    }

    @Test
    void parseJson_missingColon_throws() {
        // key 闭合后缺少冒号
        assertThatThrownBy(() -> parser.parseJson("{\"foo\" \"bar\"}"))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("缺少冒号");
    }

    @Test
    void parseJson_nestedArrayValue_parsedButIgnored() {
        // 值位置出现嵌套数组字面量，触发 parseJsonArray 递归分支
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "tags": ["a", "b"]}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("x");
    }

    @Test
    void parseJson_nestedObjectValue_parsedButIgnored() {
        // 值位置出现嵌套对象字面量，触发 parseJsonObject 递归分支
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "meta": {"k": "v"}}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("x");
    }

    @Test
    void parseJson_configurationItemsNonList_returnsNull() {
        // configuration.items 非 List -> parseConfiguration 返回 null
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "configuration": {"items": "notalist"}}
                """;
        assertThat(parser.parseJson(json).configuration()).isNull();
    }

    @Test
    void parseJson_configurationItemsEmptyList_returnsNull() {
        // configuration.items 为空 List -> parseConfiguration 返回 null
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "configuration": {"items": []}}
                """;
        assertThat(parser.parseJson(json).configuration()).isNull();
    }

    @Test
    void descriptorParseException_causeConstructor_preservesCause() {
        RuntimeException cause = new RuntimeException("root");
        DescriptorParseException ex = new DescriptorParseException("boom", cause);
        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void parseJson_emptyObject_throwsMissingRequired() {
        // parseJsonObject 内 inner 为空 -> 提前返回空 map，随后 requiredString 抛缺少必需字段
        assertThatThrownBy(() -> parser.parseJson("{}"))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("缺少必需字段");
    }

    @Test
    void parseJson_unquotedNonNumericScalar_parsesAsString() {
        // 值位置未加引号的非法标量 -> parseScalar 的 long/double 解析均失败，回退原样字符串
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "weird": abcXYZ}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("x");
    }

    @Test
    void parseJson_arrayWithUnquotedScalars_parsedByParseScalar() {
        // 数组内未加引号的 true/false/null 与非法标量走 parseScalar 分支
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "arr": [true, false, null, abcXYZ]}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("x");
    }

    @Test
    void parseJson_nestedArrayAndObjectInArray() {
        // 数组内嵌套数组与对象，触发 parseJsonArray/parseJsonObject 的递归分支
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "nested": [[1, 2], {"k": "v"}]}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("x");
    }

    @Test
    void parseJson_providesSingleString_becomesSingletonList() {
        // provides 为非列表字符串 -> toStringList 的单元素分支
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "provides": "single"}
                """;
        assertThat(parser.parseJson(json).provides()).containsExactly("single");
    }

    @Test
    void parseJson_requiresNonList_ignored() {
        // requires 为非列表值 -> parseRequires 的 else 分支返回空列表
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "requires": 5}
                """;
        assertThat(parser.parseJson(json).requires()).isEmpty();
    }

    @Test
    void parseJson_extensionPointsNonList_ignored() {
        // extensionPoints 为非列表值 -> 返回空列表
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "extensionPoints": "ep"}
                """;
        assertThat(parser.parseJson(json).extensionPoints()).isEmpty();
    }

    @Test
    void parseJson_extensionsNonList_ignored() {
        // extensions 为非列表值 -> 返回空列表
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "extensions": "ex"}
                """;
        assertThat(parser.parseJson(json).extensions()).isEmpty();
    }

    @Test
    void parseJson_configurationNonMap_returnsNull() {
        // configuration 为非 Map 值 -> parseConfiguration 返回 null
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "configuration": "x"}
                """;
        assertThat(parser.parseJson(json).configuration()).isNull();
    }

    @Test
    void parseJson_escapeCharInsideBracket_skipsTwoChars() {
        // 数组内出现反斜杠转义（非字符串内）：触发 findMatchingBracket 的转义跳过分支
        String json = """
                {"pluginId": "x", "version": "1.0", "pluginClass": "c.X", "arr": [1, 2\\3]}
                """;
        PluginDescriptor d = parser.parseJson(json);
        assertThat(d.pluginId()).isEqualTo("x");
    }
}
