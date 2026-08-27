package cn.jowen.framework.plugin.registry;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionPointTest {

    @Test
    void of_createsExtensionPoint() {
        ExtensionPoint point = ExtensionPoint.of("my-point", "com.example.MyPoint");
        assertThat(point.id()).isEqualTo("my-point");
        assertThat(point.type()).isEqualTo("com.example.MyPoint");
        assertThat(point.description()).isEqualTo("");
        assertThat(point.singleton()).isFalse();
    }

    @Test
    void constructor_full() {
        ExtensionPoint point = new ExtensionPoint("id", "type", "desc", true);
        assertThat(point.id()).isEqualTo("id");
        assertThat(point.type()).isEqualTo("type");
        assertThat(point.description()).isEqualTo("desc");
        assertThat(point.singleton()).isTrue();
    }
}
