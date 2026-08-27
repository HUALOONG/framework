package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.core.spi.SPIImplementation;
import cn.jowen.framework.plugin.descriptor.PluginJsonDescriptorParser.DescriptorParseException;
import org.jspecify.annotations.NullMarked;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 从 JAR 内 {@code META-INF/plugin/plugin.yaml} 解析 {@link PluginDescriptor}。
 *
 * <p>经 SnakeYAML 解析为映射后直接构建描述符（与 JSON 解析器字段语义一致，兼容旧/新命名）。
 * SnakeYAML 为可选依赖，仅在调用本类时才需要。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
@SPIImplementation(name = "yaml", order = 20)
public final class PluginYamlDescriptorParser implements PluginDescriptorLoader {

    private static final String PLUGIN_YAML_PATH = "META-INF/plugin/plugin.yaml";

    @Override
    public List<String> supportedExtensions() {
        return List.of("yaml", "yml");
    }

    @Override
    public PluginDescriptor load(Path jarPath) throws IOException {
        String content;
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(PLUGIN_YAML_PATH);
            if (entry == null) {
                throw new DescriptorParseException("JAR 中不存在 " + PLUGIN_YAML_PATH);
            }
            try (InputStream is = jarFile.getInputStream(entry)) {
                content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        return parseYaml(content);
    }

    /**
     * 直接解析 YAML 字符串。
     *
     * @param yaml YAML 内容，不可为 {@code null}
     * @return 插件描述符
     */
    public PluginDescriptor parseYaml(String yaml) {
        Object parsed = new Yaml().load(yaml);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new DescriptorParseException("plugin.yaml 根节点必须是映射对象");
        }
        Map<String, Object> map = toStringKeyedMap(root);

        String pluginId = required(map, "pluginId", "id");
        String version = required(map, "version");
        String pluginClass = required(map, "pluginClass", "class");
        String pluginName = asString(map.getOrDefault("pluginName", pluginId));
        String description = asString(map.getOrDefault("description", ""));
        String author = asString(map.getOrDefault("author", ""));
        String license = asString(map.getOrDefault("license", ""));

        List<PluginDependency> requires = parseRequires(map.get("requires"));
        List<PluginDependency> optionalRequires = parseRequires(map.get("optionalRequires"));
        List<String> provides = toStringList(map.get("provides"));
        List<ExtensionPointDescriptor> extensionPoints = parseExtensionPoints(map.get("extensionPoints"));
        List<ExtensionDescriptor> extensions = parseExtensions(map.get("extensions"));
        PluginConfigurationDescriptor configuration = parseConfiguration(map.get("configuration"));
        boolean enabledByDefault = Boolean.TRUE.equals(map.get("enabledByDefault"));

        return new PluginDescriptor(pluginId, pluginName, version, description, author, license, pluginClass,
                requires, optionalRequires, provides, extensionPoints, extensions, configuration, enabledByDefault);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toStringKeyedMap(Map<?, ?> root) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : root.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return result;
    }

    private static String required(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        throw new DescriptorParseException("缺少必需字段：" + keys[0]);
    }

    private static String asString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static List<String> toStringList(Object value) {
        if (value == null) {
            return Collections.emptyList();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return Collections.singletonList(value.toString());
    }

    @SuppressWarnings("unchecked")
    private static List<PluginDependency> parseRequires(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<PluginDependency> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String s) {
                result.add(new PluginDependency(s, new VersionRange("[0.0.0,999.999.999]"), false));
            } else if (item instanceof Map<?, ?> m) {
                result.add(PluginDependency.fromMap(toStringKeyedMap(m)));
            }
        }
        return result;
    }

    private static List<ExtensionPointDescriptor> parseExtensionPoints(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ExtensionPointDescriptor> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                Map<String, Object> map = toStringKeyedMap(m);
                result.add(new ExtensionPointDescriptor(
                        asString(map.get("id")),
                        asString(map.get("interfaceName")),
                        Boolean.TRUE.equals(map.get("singleton"))));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<ExtensionDescriptor> parseExtensions(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<ExtensionDescriptor> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(ExtensionDescriptor.fromMap(toStringKeyedMap(m)));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static @org.jspecify.annotations.Nullable PluginConfigurationDescriptor parseConfiguration(Object value) {
        if (!(value instanceof Map<?, ?> c)) {
            return null;
        }
        Object items = toStringKeyedMap(c).get("items");
        if (!(items instanceof List<?> list) || list.isEmpty() || !(list.getFirst() instanceof Map<?, ?> first)) {
            return null;
        }
        Map<String, Object> item = toStringKeyedMap(first);
        return new PluginConfigurationDescriptor(
                asString(item.get("key")),
                asString(item.get("type")),
                asString(item.get("defaultValue")),
                Boolean.TRUE.equals(item.get("required")),
                asString(item.get("description")));
    }
}