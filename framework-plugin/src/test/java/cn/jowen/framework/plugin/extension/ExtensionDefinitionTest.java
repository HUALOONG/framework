package cn.jowen.framework.plugin.extension;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionDefinitionTest {

    @Test
    void instantiate_validClass_returnsInstance() {
        ExtensionDefinition def = new ExtensionDefinition(
                "ext1", "ep1", "java.util.HashMap", 0, null);
        Object instance = def.instantiate(getClass().getClassLoader());
        assertThat(instance).isNotNull();
        assertThat(instance).isInstanceOf(java.util.HashMap.class);
    }

    @Test
    void instantiate_invalidClass_returnsNull() {
        ExtensionDefinition def = new ExtensionDefinition(
                "ext1", "ep1", "com.nonexistent.Fake", 0, null);
        assertThat(def.instantiate(getClass().getClassLoader())).isNull();
    }

    @Test
    void fields() {
        ExtensionDefinition def = new ExtensionDefinition(
                "ext1", "ep1", "com.example.Ext", 5, null);
        assertThat(def.id()).isEqualTo("ext1");
        assertThat(def.extensionPointId()).isEqualTo("ep1");
        assertThat(def.className()).isEqualTo("com.example.Ext");
        assertThat(def.order()).isEqualTo(5);
        assertThat(def.properties()).isNull();
    }
}
