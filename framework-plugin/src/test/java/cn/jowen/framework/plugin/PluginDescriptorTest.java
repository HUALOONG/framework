package cn.jowen.framework.plugin;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginDescriptorTest {

    @Test
    void ofFactoryMethod() {
        PluginDescriptor desc = PluginDescriptor.of("id1", "1.0.0", "com.example.X");
        assertThat(desc.id()).isEqualTo("id1");
        assertThat(desc.version()).isEqualTo("1.0.0");
        assertThat(desc.className()).isEqualTo("com.example.X");
        assertThat(desc.description()).isEmpty();
        assertThat(desc.dependencies()).isEmpty();
        assertThat(desc.exportedPackages()).isEmpty();
        assertThat(desc.springEnabled()).isFalse();
    }

    @Test
    void fullConstructor() {
        var deps = java.util.List.of("dep-a");
        var exports = java.util.List.of("com.example.api");
        PluginDescriptor desc = new PluginDescriptor("id1", "1.0.0", "com.example.X",
                "desc", deps, exports, true);
        assertThat(desc.dependencies()).containsExactly("dep-a");
        assertThat(desc.exportedPackages()).containsExactly("com.example.api");
        assertThat(desc.springEnabled()).isTrue();
    }

    @Test
    void nullDependenciesBecomeEmptyList() {
        PluginDescriptor desc = new PluginDescriptor("id", "1.0.0", "com.x.X", "", null);
        assertThat(desc.dependencies()).isEmpty();
    }
}
