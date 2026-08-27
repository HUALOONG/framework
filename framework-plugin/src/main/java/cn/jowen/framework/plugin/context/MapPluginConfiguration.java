package cn.jowen.framework.plugin.context;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 {@link Map} 的 {@link PluginConfiguration} 默认实现，按 key 取值并做类型转换。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class MapPluginConfiguration implements PluginConfiguration {

    private final Map<String, Object> values;

    /**
     * 构造空配置。
     */
    public MapPluginConfiguration() {
        this.values = Map.of();
    }

    /**
     * 构造配置。
     *
     * @param values 配置键值，不可为 {@code null}
     */
    public MapPluginConfiguration(Map<String, Object> values) {
        Objects.requireNonNull(values, "values must not be null");
        this.values = Map.copyOf(values);
    }

    @Override
    public @Nullable String getString(String key) {
        Objects.requireNonNull(key, "key must not be null");
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        Objects.requireNonNull(key, "key must not be null");
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException ignored) {
                // 非数字回退默认值
            }
        }
        return defaultValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        Objects.requireNonNull(key, "key must not be null");
        Object value = values.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return defaultValue;
    }

    @Override
    public List<String> getList(String key) {
        Objects.requireNonNull(key, "key must not be null");
        Object value = values.get(key);
        if (value instanceof List<?> list) {
            return list.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of();
    }

    @Override
    public Map<String, String> getMap(String key) {
        Objects.requireNonNull(key, "key must not be null");
        Object value = values.get(key);
        if (value instanceof Map<?, ?> map) {
            Map<String, String> result = new java.util.LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            return result;
        }
        return Map.of();
    }

    @Override
    public Map<String, Object> getAll() {
        return values;
    }
}