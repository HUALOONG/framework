package cn.jowen.framework.plugin.descriptor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 描述符解析的边界分支契约：预发布版本比较、非法版本区间、默认 {@code validate} 委托，
 * 以及 JSON/YAML 解析器的容错路径。
 *
 * <p>这些分支一旦失效，插件装载会在"几乎合法的描述符"上静默降级或直接抛出不清晰异常。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class DescriptorGapCoverageTest {

    @Test
    void comparePreRelease_twoPrereleases_compareLexicographically() {
        PluginVersion alpha = PluginVersion.parse("1.0.0-alpha");
        PluginVersion beta = PluginVersion.parse("1.0.0-beta");

        assertThat(alpha.compareTo(beta)).isNegative();
        assertThat(beta.compareTo(alpha)).isPositive();
    }

    @Test
    void versionRange_illegalFormat_throwsIllegalArgument() {
        assertThatThrownBy(() -> new VersionRange("[1.x,2.0]"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法版本号格式");
    }

    @Test
    void descriptorLoader_defaultValidate_delegatesToDescriptor() {
        PluginDescriptor descriptor = PluginDescriptor.of("demo", "1.0.0", "com.demo.DemoPlugin");

        assertThat(new PluginJsonDescriptorParser().validate(descriptor)).isNotNull();
    }

    @Test
    void yamlParser_supportedExtensions_yamlAndYml() {
        assertThat(new PluginYamlDescriptorParser().supportedExtensions())
                .containsExactly("yaml", "yml");
    }

    @Test
    void yamlParser_providesAsScalar_wrappedIntoSingleItemList() {
        PluginDescriptor d = new PluginYamlDescriptorParser().parseYaml("""
                pluginId: demo
                pluginName: Demo
                version: 1.0.0
                pluginClass: com.demo.DemoPlugin
                provides: single-capability
                """);

        assertThat(d.provides()).containsExactly("single-capability");
    }

    @Test
    void yamlParser_configurationWithoutItems_returnsNullConfiguration() {
        PluginDescriptor d = new PluginYamlDescriptorParser().parseYaml("""
                pluginId: demo
                pluginName: Demo
                version: 1.0.0
                pluginClass: com.demo.DemoPlugin
                configuration: {enabled: true}
                """);

        assertThat(d.configuration()).isNull();
    }

    @Test
    void jsonParser_arrayWithoutClosingBracket_throws() {
        // 数组括号不闭合：findMatchingBracket 返回 -1 后切片越界，统一收敛为运行时异常
        assertThatThrownBy(() -> new PluginJsonDescriptorParser().parseJson("""
                {"id":"demo","class":"com.demo.DemoPlugin","version":"1.0.0","requires":[1,2}
                """))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void jsonParser_escapedQuoteInsideString_skippedWhileScanning() {
        // 字符串字面量内的转义引号不应被误判为字符串结尾
        String json = "{\"id\":\"demo\",\"class\":\"com.demo.DemoPlugin\",\"version\":\"1.0.0\","
                + "\"provides\":[\"a\\\"b\"]}";

        PluginDescriptor d = new PluginJsonDescriptorParser().parseJson(json);

        assertThat(d.provides()).hasSize(1);
        assertThat(d.provides().get(0)).contains("b");
    }

    @Test
    void jsonParser_unterminatedStringInsideArray_throws() {
        assertThatThrownBy(() -> new PluginJsonDescriptorParser().parseJson(
                "{\"id\":\"demo\",\"class\":\"com.demo.DemoPlugin\","
                        + "\"version\":\"1.0.0\",\"provides\":[\"unterminated}"))
                .isInstanceOf(RuntimeException.class);
    }
}
