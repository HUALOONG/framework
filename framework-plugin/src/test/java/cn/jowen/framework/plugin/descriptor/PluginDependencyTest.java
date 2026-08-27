package cn.jowen.framework.plugin.descriptor;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PluginDependencyTest {

    @Test
    void fromMap_withAllFields() {
        Map<String, Object> map = Map.of(
                "pluginId", "plugin-b",
                "versionRange", "[1.0.0,2.0.0]",
                "optional", true
        );
        PluginDependency dep = PluginDependency.fromMap(map);
        assertThat(dep.pluginId()).isEqualTo("plugin-b");
        assertThat(dep.versionRange().getLower()).isEqualTo("1.0.0");
        assertThat(dep.optional()).isTrue();
    }

    @Test
    void fromMap_nullVersionRange_usesDefaultRange() {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("pluginId", "plugin-b");
        map.put("versionRange", (String) null);
        map.put("optional", false);
        PluginDependency dep = PluginDependency.fromMap(map);
        assertThat(dep.pluginId()).isEqualTo("plugin-b");
        assertThat(dep.versionRange()).isNotNull();
        assertThat(dep.versionRange().getLower()).isEqualTo("0.0.0");
        assertThat(dep.optional()).isFalse();
    }

    @Test
    void fromMap_missingOptional_defaultsToFalse() {
        Map<String, Object> map = Map.of(
                "pluginId", "plugin-b",
                "versionRange", "[0.0.0,999.999.999]"
        );
        PluginDependency dep = PluginDependency.fromMap(map);
        assertThat(dep.optional()).isFalse();
    }
}
