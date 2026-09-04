package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.plugin.descriptor.PluginYamlDescriptorParser;
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

class PluginYamlDescriptorParserTest {

    private final PluginYamlDescriptorParser parser = new PluginYamlDescriptorParser();

    @Test
    void parseYaml_fullDescriptor() {
        String yaml = """
                pluginId: demo
                pluginName: Demo Plugin
                version: 1.2.3
                description: demo desc
                author: me
                license: MIT
                pluginClass: com.demo.DemoPlugin
                enabledByDefault: true
                requires:
                  - base
                  - pluginId: db
                    versionRange: "[1.0,2.0)"
                optionalRequires:
                  - opt
                provides:
                  - cap1
                  - cap2
                extensionPoints:
                  - id: ep1
                    interfaceName: com.demo.Ep
                    singleton: true
                extensions:
                  - id: ex1
                    extensionPointId: ep1
                    className: com.demo.Ex
                    order: 5
                configuration:
                  items:
                    - key: k1
                      type: string
                      defaultValue: d
                      required: true
                      description: desc
                """;
        PluginDescriptor d = parser.parseYaml(yaml);
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
    void parseYaml_oldFormat_compatible() {
        String yaml = """
                id: legacy
                class: com.legacy.Legacy
                version: 0.9.0
                """;
        PluginDescriptor d = parser.parseYaml(yaml);
        assertThat(d.pluginId()).isEqualTo("legacy");
        assertThat(d.pluginClass()).isEqualTo("com.legacy.Legacy");
        assertThat(d.configuration()).isNull();
        assertThat(d.extensionPoints()).isEmpty();
    }

    @Test
    void parseYaml_disabledByDefault_false() {
        String yaml = """
                pluginId: x
                version: 1.0
                pluginClass: c.X
                enabledByDefault: false
                """;
        assertThat(parser.parseYaml(yaml).enabledByDefault()).isFalse();
    }

    @Test
    void parseYaml_missingRequiredField_throws() {
        String yaml = """
                version: 1.0
                pluginClass: c.X
                """;
        assertThatThrownBy(() -> parser.parseYaml(yaml))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("缺少必需字段");
    }

    @Test
    void parseYaml_rootNotMap_throws() {
        assertThatThrownBy(() -> parser.parseYaml("- a\n- b\n"))
                .isInstanceOf(DescriptorParseException.class)
                .hasMessageContaining("根节点必须是映射对象");
    }

    @Test
    void load_fromJar_readsPluginYaml() throws Exception {
        String yaml = """
                pluginId: jarred
                version: 2.0.0
                pluginClass: c.Jar
                """;
        Path jar = Files.createTempFile("plugin", ".jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar));
             ByteArrayInputStream in = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            jos.putNextEntry(new JarEntry("META-INF/plugin/plugin.yaml"));
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
}
