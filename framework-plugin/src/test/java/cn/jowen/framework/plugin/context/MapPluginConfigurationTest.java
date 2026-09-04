package cn.jowen.framework.plugin.context;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MapPluginConfiguration} 全方法覆盖测试：类型转换、回退默认值与空值容错。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class MapPluginConfigurationTest {

    private static Map<String, Object> sample() {
        Map<String, Object> values = new HashMap<>();
        values.put("name", "demo");
        values.put("count", 42);
        values.put("ratio", 3.7d);
        values.put("textCount", "7");
        values.put("badCount", "abc");
        values.put("enabled", Boolean.TRUE);
        values.put("textEnabled", "true");
        values.put("tags", java.util.Arrays.asList("a", "b", null, "c"));
        values.put("numbers", List.of(1, 2));
        Map<String, Object> nested = new HashMap<>();
        nested.put("k1", "v1");
        nested.put("k2", 2);
        values.put("meta", nested);
        return values;
    }

    @Test
    void getString_variousTypes() {
        MapPluginConfiguration config = new MapPluginConfiguration(sample());

        assertThat(config.getString("name")).isEqualTo("demo");
        assertThat(config.getString("count")).isEqualTo("42");
        assertThat(config.getString("ratio")).isEqualTo("3.7");
        assertThat(config.getString("missing")).isNull();
    }

    @Test
    void getInt_numberStringAndFallback() {
        MapPluginConfiguration config = new MapPluginConfiguration(sample());

        assertThat(config.getInt("count", -1)).isEqualTo(42);
        assertThat(config.getInt("ratio", -1)).isEqualTo(3);
        assertThat(config.getInt("textCount", -1)).isEqualTo(7);
        // 非数字字符串与非数值类型回退默认值
        assertThat(config.getInt("badCount", -1)).isEqualTo(-1);
        assertThat(config.getInt("name", -1)).isEqualTo(-1);
        assertThat(config.getInt("missing", 99)).isEqualTo(99);
    }

    @Test
    void getBoolean_booleanStringAndFallback() {
        MapPluginConfiguration config = new MapPluginConfiguration(sample());

        assertThat(config.getBoolean("enabled", false)).isTrue();
        assertThat(config.getBoolean("textEnabled", false)).isTrue();
        // 非 Boolean 非 String 类型与缺失键回退默认值
        assertThat(config.getBoolean("count", true)).isTrue();
        assertThat(config.getBoolean("missing", true)).isTrue();
        assertThat(config.getBoolean("missing", false)).isFalse();
    }

    @Test
    void getList_filtersNullAndStringifies() {
        MapPluginConfiguration config = new MapPluginConfiguration(sample());

        assertThat(config.getList("tags")).containsExactly("a", "b", "c");
        assertThat(config.getList("numbers")).containsExactly("1", "2");
        // 非 List 类型与缺失键返回空列表
        assertThat(config.getList("name")).isEmpty();
        assertThat(config.getList("missing")).isEmpty();
    }

    @Test
    void getMap_stringifiesEntries() {
        MapPluginConfiguration config = new MapPluginConfiguration(sample());

        Map<String, String> meta = config.getMap("meta");
        assertThat(meta).containsEntry("k1", "v1").containsEntry("k2", "2");
        // 非 Map 类型与缺失键返回空 Map
        assertThat(config.getMap("name")).isEmpty();
        assertThat(config.getMap("missing")).isEmpty();
    }

    @Test
    void constructors_defensiveCopyAndNullCheck() {
        Map<String, Object> values = new HashMap<>();
        values.put("k", "v");

        MapPluginConfiguration config = new MapPluginConfiguration(values);
        values.put("k2", "v2");
        // 构造时快照，后续原 Map 变更不影响配置
        assertThat(config.getAll()).containsOnlyKeys("k");

        assertThat(new MapPluginConfiguration().getAll()).isEmpty();
        assertThatThrownBy(() -> new MapPluginConfiguration(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullKeys_rejected() {
        MapPluginConfiguration config = new MapPluginConfiguration();

        assertThatThrownBy(() -> config.getString(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> config.getInt(null, 0)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> config.getBoolean(null, false))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> config.getList(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> config.getMap(null)).isInstanceOf(NullPointerException.class);
    }
}
