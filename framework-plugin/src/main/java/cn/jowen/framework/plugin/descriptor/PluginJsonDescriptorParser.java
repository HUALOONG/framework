package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 从 JAR 内 {@code META-INF/plugin/plugin.json} 解析为 {@link PluginDescriptor}。
 *
 * <p>同时兼容旧格式（id/version/class/dependencies）和新格式（pluginId/pluginName/pluginClass/requires...）。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginJsonDescriptorParser implements PluginDescriptorLoader {

    private static final String PLUGIN_JSON_PATH = "META-INF/plugin/plugin.json";

    private static String readJarResource(Path jarPath, String entryName) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry(entryName);
            if (entry == null) {
                throw new DescriptorParseException("JAR 中不存在 " + entryName);
            }
            try (InputStream is = jarFile.getInputStream(entry);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJsonObject(String json) throws DescriptorParseException {
        String trimmed = json.trim();
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw new DescriptorParseException("无效的 JSON 对象：" + json);
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        Map<String, Object> result = new LinkedHashMap<>();
        if (inner.isEmpty()) return result;

        int pos = 0;
        while (pos < inner.length()) {
            pos = skipWhitespace(inner, pos);
            if (pos >= inner.length()) break;
            int keyStart = skipQuotes(inner, pos);
            if (keyStart < 0) throw new DescriptorParseException("缺少 key 起始引号");
            int keyEnd = findClosingQuote(inner, keyStart + 1);
            if (keyEnd < 0) throw new DescriptorParseException("key 未闭合：" + inner.substring(keyStart));
            String key = inner.substring(keyStart + 1, keyEnd);
            pos = keyEnd + 1;
            pos = skipWhitespace(inner, pos);
            if (pos >= inner.length() || inner.charAt(pos) != ':') {
                throw new DescriptorParseException("缺少冒号");
            }
            pos++;
            pos = skipWhitespace(inner, pos);
            Object value;
            if (inner.charAt(pos) == '"') {
                int end = findClosingQuote(inner, pos + 1);
                value = inner.substring(pos + 1, end);
                pos = end + 1;
            } else if (inner.regionMatches(true, pos, "true", 0, 4)) {
                value = true;
                pos += 4;
            } else if (inner.regionMatches(true, pos, "false", 0, 5)) {
                value = false;
                pos += 5;
            } else if (inner.regionMatches(true, pos, "null", 0, 4)) {
                value = null;
                pos += 4;
            } else if (inner.charAt(pos) == '[') {
                int end = findMatchingBracket(inner, pos, '[', ']');
                value = parseJsonArray(inner.substring(pos, end + 1));
                pos = end + 1;
            } else if (inner.charAt(pos) == '{') {
                int end = findMatchingBracket(inner, pos, '{', '}');
                value = parseJsonObject(inner.substring(pos, end + 1));
                pos = end + 1;
            } else {
                int end = pos;
                while (end < inner.length() && ",}]"
                        .indexOf(inner.charAt(end)) < 0) end++;
                value = parseScalar(inner.substring(pos, end).trim());
                pos = end;
            }
            result.put(key, value);
            pos = skipWhitespace(inner, pos);
            if (pos < inner.length() && inner.charAt(pos) == ',') pos++;
        }
        return result;
    }

    private static List<Object> parseJsonArray(String json) throws DescriptorParseException {
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new DescriptorParseException("无效的 JSON 数组：" + json);
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        List<Object> result = new ArrayList<>();
        if (inner.isEmpty()) return result;
        int pos = 0;
        while (pos < inner.length()) {
            pos = skipWhitespace(inner, pos);
            if (pos >= inner.length()) break;
            char c = inner.charAt(pos);
            Object value;
            if (c == '"') {
                int end = findClosingQuote(inner, pos + 1);
                value = inner.substring(pos + 1, end);
                pos = end + 1;
            } else if (c == '{') {
                int end = findMatchingBracket(inner, pos, '{', '}');
                value = parseJsonObject(inner.substring(pos, end + 1));
                pos = end + 1;
            } else if (c == '[') {
                int end = findMatchingBracket(inner, pos, '[', ']');
                value = parseJsonArray(inner.substring(pos, end + 1));
                pos = end + 1;
            } else {
                int end = pos;
                while (end < inner.length() && ',' != inner.charAt(end)) end++;
                value = parseScalar(inner.substring(pos, end).trim());
                pos = end;
            }
            result.add(value);
            pos = skipWhitespace(inner, pos);
            if (pos < inner.length() && inner.charAt(pos) == ',') pos++;
        }
        return result;
    }

    /**
     * 解析标量字面量：true/false/null/数字，否则按原样字符串返回。
     */
    private static Object parseScalar(String token) {
        if ("true".equalsIgnoreCase(token)) return Boolean.TRUE;
        if ("false".equalsIgnoreCase(token)) return Boolean.FALSE;
        if ("null".equalsIgnoreCase(token)) return null;
        try {
            return Long.parseLong(token);
        } catch (NumberFormatException e) {
            try {
                return Double.parseDouble(token);
            } catch (NumberFormatException e2) {
                return token;
            }
        }
    }

    private static int skipWhitespace(String s, int pos) {
        while (pos < s.length() && Character.isWhitespace(s.charAt(pos))) pos++;
        return pos;
    }

    private static int skipQuotes(String s, int pos) {
        return s.charAt(pos) == '"' ? pos : -1;
    }

    private static int findClosingQuote(String s, int start) {
        int i = start;
        while (i < s.length()) {
            if (s.charAt(i) == '\\') {
                i += 2;
                continue;
            }
            if (s.charAt(i) == '"') return i;
            i++;
        }
        return -1;
    }

    private static int findMatchingBracket(String s, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
                continue;
            }
            // 字符串字面量内的括号不计入深度匹配
            if (c == '"') {
                i = findClosingQuote(s, i + 1);
                if (i < 0) return -1;
                continue;
            }
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    // ----- file reading -----

    private static String requiredString(Map<String, Object> map, String... alternateKeys) throws DescriptorParseException {
        for (String key : alternateKeys) {
            Object v = map.get(key);
            if (v != null) return v.toString();
        }
        throw new DescriptorParseException("缺少必需字段：" + alternateKeys[0]);
    }

    // ----- simple JSON object parser -----

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object val) {
        if (val == null) return Collections.emptyList();
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                if (item != null) result.add(item.toString());
            }
            return result;
        }
        return Collections.singletonList(val.toString());
    }

    @Override
    public List<String> supportedExtensions() {
        return List.of("json");
    }

    @Override
    public PluginDescriptor load(Path jarPath) throws IOException {
        String content = readJarResource(jarPath, PLUGIN_JSON_PATH);
        return parseJson(content);
    }

    /**
     * 直接从 JSON 字符串解析。
     *
     * @param json JSON 字符串，不可为 {@code null}
     * @return 插件描述符
     * @throws DescriptorParseException JSON 格式非法时抛出
     */
    public PluginDescriptor parseJson(String json) throws DescriptorParseException {
        Map<String, Object> map = parseJsonObject(json);

        // 兼容新旧字段名
        String pluginId = requiredString(map, "pluginId", "id");
        String version = requiredString(map, "version");
        String pluginClass = requiredString(map, "pluginClass", "class");
        String pluginName = (String) map.getOrDefault("pluginName", pluginId);
        String description = (String) map.getOrDefault("description", "");
        String author = (String) map.getOrDefault("author", "");
        String license = (String) map.getOrDefault("license", "");

        // requires: 兼容旧格式（List<String>）和新格式（List<{pluginId, versionRange}>）
        List<PluginDependency> requires = parseRequires(map.get("requires"));
        List<PluginDependency> optionalRequires = parseRequires(map.get("optionalRequires"));

        // provides
        List<String> provides = toStringList(map.get("provides"));

        // extensionPoints
        List<ExtensionPointDescriptor> extensionPoints = parseExtensionPointDescriptors(map.get("extensionPoints"));

        // extensions
        List<ExtensionDescriptor> extensions = parseExtensionDescriptors(map.get("extensions"));

        // configuration
        PluginConfigurationDescriptor configuration = parseConfiguration(map.get("configuration"));

        boolean enabledByDefault = Boolean.TRUE.equals(map.get("enabledByDefault"));

        return new PluginDescriptor(
                pluginId, pluginName, version, description, author, license, pluginClass,
                requires, optionalRequires, provides,
                extensionPoints, extensions, configuration, enabledByDefault
        );
    }

    @SuppressWarnings("unchecked")
    private List<PluginDependency> parseRequires(Object val) {
        if (val == null) return List.of();
        if (val instanceof List<?> list) {
            List<PluginDependency> result = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof String s) {
                    // 旧格式：纯字符串作为 pluginId，版本范围默认为全部
                    result.add(new PluginDependency(s, new VersionRange("[0.0.0,999.999.999]"), false));
                } else if (item instanceof Map<?, ?> m) {
                    result.add(PluginDependency.fromMap((Map<String, Object>) m));
                }
            }
            return result;
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<ExtensionPointDescriptor> parseExtensionPointDescriptors(Object val) {
        if (val == null || !(val instanceof List<?> list)) return List.of();
        List<ExtensionPointDescriptor> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(new ExtensionPointDescriptor(
                        (String) m.get("id"),
                        (String) m.get("interfaceName"),
                        Boolean.TRUE.equals(m.get("singleton"))
                ));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<ExtensionDescriptor> parseExtensionDescriptors(Object val) {
        if (val == null || !(val instanceof List<?> list)) return List.of();
        List<ExtensionDescriptor> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> m) {
                result.add(ExtensionDescriptor.fromMap((Map<String, Object>) m));
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private PluginConfigurationDescriptor parseConfiguration(Object val) {
        if (val == null || !(val instanceof Map<?, ?> m)) return null;
        Object items = m.get("items");
        if (!(items instanceof List<?> list) || list.isEmpty()) return null;
        Map<String, Object> item = (Map<String, Object>) list.getFirst();
        return new PluginConfigurationDescriptor(
                (String) item.get("key"),
                (String) item.get("type"),
                (String) item.get("defaultValue"),
                Boolean.TRUE.equals(item.get("required")),
                (String) item.get("description")
        );
    }

    public static final class DescriptorParseException extends RuntimeException {
        public DescriptorParseException(String message) {
            super(message);
        }

        public DescriptorParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
