package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.plugin.PluginLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionRegistryTest {

    private ExtensionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ExtensionRegistry();
    }

    @Test
    void registerAndGetExtensions() {
        registry.register(new StringTruncator());
        registry.register(new StringReverser());

        var extensions = registry.getExtensions(StringTransformer.class);
        assertThat(extensions).hasSize(2);
        assertThat(extensions.get(0)).isInstanceOf(StringReverser.class); // order=1 before order=10
    }

    @Test
    void getExtensionByName() {
        registry.register(new StringTruncator());
        registry.register(new StringReverser());

        var ext = registry.getExtension(StringTransformer.class, "reverser");
        assertThat(ext).isInstanceOf(StringReverser.class);
    }

    @Test
    void emptyWhenNoRegistration() {
        assertThat(registry.getExtensions(StringTransformer.class)).isEmpty();
    }

    @Test
    void registerWithoutAnnotationThrows() {
        assertThatThrownBy(() -> registry.register(new Object()))
                .isInstanceOf(PluginLoader.PluginException.class);
    }

    // ---- test fixtures ----

    @ExtensionPoint
    interface StringTransformer {
        String transform(String input);
    }

    @Extension(point = StringTransformer.class, name = "truncator", order = 10)
    static class StringTruncator implements StringTransformer {
        @Override
        public String transform(String input) {
            return input == null ? null : input.substring(0, Math.min(3, input.length()));
        }
    }

    @Extension(point = StringTransformer.class, name = "reverser", order = 1)
    static class StringReverser implements StringTransformer {
        @Override
        public String transform(String input) {
            return input == null ? null : new StringBuilder(input).reverse().toString();
        }
    }
}
