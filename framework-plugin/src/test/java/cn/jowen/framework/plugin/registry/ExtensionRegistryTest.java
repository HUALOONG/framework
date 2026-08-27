package cn.jowen.framework.plugin.registry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

class ExtensionRegistryTest {

    private ExtensionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ExtensionRegistry();
    }

    @Test
    void register_andGetExtension() {
        Extension ext = new Extension("e1", "ep1", new Object(), 0, "p1", null);
        registry.register(ext);
        @SuppressWarnings("unchecked")
        List<Object> list = (List<Object>) (List<?>) registry.getExtensions("ep1");
        assertThat(list).hasSize(1);
    }

    @Test
    void register_duplicateId_throwsIllegalArgument() {
        Extension ext1 = new Extension("e1", "ep1", new Object(), 0, "p1", null);
        Extension ext2 = new Extension("e1", "ep1", new Object(), 1, "p1", null);
        registry.register(ext1);
        assertThatThrownBy(() -> registry.register(ext2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("重复");
    }

    @Test
    void register_null_throwsIllegalArgument() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void register_multipleExtensions_sameExtensionPoint() {
        registry.register(new Extension("e1", "ep1", new Object(), 10, "p1", null));
        registry.register(new Extension("e2", "ep1", new Object(), 5, "p1", null));
        @SuppressWarnings("unchecked")
        List<Extension> list = (List<Extension>) (List<?>) registry.getExtensions("ep1");
        assertThat(list).hasSize(2);
        assertThat(list.getFirst().id()).isEqualTo("e2");
        assertThat(list.getLast().id()).isEqualTo("e1");
    }

    @Test
    void unregister_removesExtension() {
        registry.register(new Extension("e1", "ep1", new Object(), 0, "p1", null));
        Extension removed = registry.unregister("ep1", "e1");
        assertThat(removed).isNotNull();
        assertThat(registry.getExtensions("ep1")).isEmpty();
    }

    @Test
    void unregister_nonExistent_returnsNull() {
        assertThat(registry.unregister("ep1", "missing")).isNull();
    }

    @Test
    void getExtensionPointIds_returnsAllRegisteredIds() {
        registry.register(new Extension("e1", "ep1", new Object(), 0, "p1", null));
        registry.register(new Extension("e2", "ep2", new Object(), 0, "p2", null));
        assertThat(registry.getExtensionPointIds()).containsExactlyInAnyOrder("ep1", "ep2");
    }

    @Test
    void clear_removesAll() {
        registry.register(new Extension("e1", "ep1", new Object(), 0, "p1", null));
        registry.clear();
        assertThat(registry.getExtensions("ep1")).isEmpty();
        assertThat(registry.getExtensionPointIds()).isEmpty();
    }

    @Test
    void getExtensions_nonExistentExtensionPoint_returnsEmpty() {
        assertThat(registry.getExtensions("nonexistent")).isEmpty();
    }
}
