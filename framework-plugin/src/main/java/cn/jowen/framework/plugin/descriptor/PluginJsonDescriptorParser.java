package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.plugin.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 从 JAR 内 {@code /META-INF/plugin/plugin.json} 解析为 {@link PluginDescriptor}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginJsonDescriptorParser {

    private static final String PLUGIN_JSON_PATH = "META-INF/plugin/plugin.json";

    public PluginDescriptor parse(JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getEntry(PLUGIN_JSON_PATH);
        if (entry == null) {
            throw new DescriptorParseException("JAR 中不存在 " + PLUGIN_JSON_PATH);
        }
        String content;
        try (InputStream is = jarFile.getInputStream(entry);
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            content = sb.toString();
        }
        return parseJson(content);
    }

    public PluginDescriptor parse(String jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            return parse(jarFile);
        }
    }

    public PluginDescriptor parseJson(String json) throws DescriptorParseException {
        Map<String, Object> map = parseJsonObject(json);
        String id = requiredString(map, "id");
        String version = requiredString(map, "version");
        String className = requiredString(map, "class");
        String description = (String) map.getOrDefault("description", "");
        List<String> dependencies = toStringList(map.get("dependencies"));
        List<String> exportedPackages = toStringList(map.get("exportedPackages"));
        boolean springEnabled = Boolean.TRUE.equals(map.get("springEnabled"));

        // PluginDescriptor is a record, create new instance with all fields
        return new PluginDescriptor(id, version, className, description,
                dependencies, exportedPackages, springEnabled);
    }

    // ----- simple JSON object parser -----

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
                value = true; pos += 4;
            } else if (inner.regionMatches(true, pos, "false", 0, 5)) {
                value = false; pos += 5;
            } else if (inner.regionMatches(true, pos, "null", 0, 4)) {
                value = null; pos += 4;
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
                while (end < inner.length() && ",}".indexOf(inner.charAt(end)) < 0) end++;
                String numStr = inner.substring(pos, end).trim();
                try { value = Long.parseLong(numStr); }
                catch (NumberFormatException e) {
                    try { value = Double.parseDouble(numStr); }
                    catch (NumberFormatException e2) { value = numStr; }
                }
                pos = end;
            }
            result.put(key, value);
            pos = skipWhitespace(inner, pos);
            if (pos < inner.length() && inner.charAt(pos) == ',') pos++;
        }
        return result;
    }

    private static List<String> parseJsonArray(String json) throws DescriptorParseException {
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new DescriptorParseException("无效的 JSON 数组：" + json);
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        List<String> result = new ArrayList<>();
        if (inner.isEmpty()) return result;
        int pos = 0;
        while (pos < inner.length()) {
            pos = skipWhitespace(inner, pos);
            if (pos >= inner.length()) break;
            if (inner.charAt(pos) == '"') {
                int end = findClosingQuote(inner, pos + 1);
                result.add(inner.substring(pos + 1, end));
                pos = end + 1;
            } else {
                int end = pos;
                while (end < inner.length() && ',' != inner.charAt(end)) end++;
                result.add(inner.substring(pos, end).trim());
                pos = end + 1;
            }
            pos = skipWhitespace(inner, pos);
            if (pos < inner.length() && inner.charAt(pos) == ',') pos++;
        }
        return result;
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
            if (s.charAt(i) == '\\') { i += 2; continue; }
            if (s.charAt(i) == '"') return i;
            i++;
        }
        return -1;
    }

    private static int findMatchingBracket(String s, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') { i++; continue; }
            if (c == open) depth++;
            else if (c == close) { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private static String requiredString(Map<String, Object> map, String key) throws DescriptorParseException {
        Object v = map.get(key);
        if (v == null) throw new DescriptorParseException("缺少必需字段：" + key);
        return v.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object val) {
        if (val == null) return Collections.emptyList();
        if (val instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) { if (item != null) result.add(item.toString()); }
            return result;
        }
        return Collections.singletonList(val.toString());
    }

    public static final class DescriptorParseException extends RuntimeException {
        public DescriptorParseException(String message) { super(message); }
        public DescriptorParseException(String message, Throwable cause) { super(message, cause); }
    }
}
