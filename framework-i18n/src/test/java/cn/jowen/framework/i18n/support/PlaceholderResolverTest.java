package cn.jowen.framework.i18n.support;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PlaceholderResolver} 测试。
 */
class PlaceholderResolverTest {

    private final PlaceholderResolver resolver = PlaceholderResolver.getInstance();

    @Test
    void resolve_singlePlaceholder_replacedWithValue() {
        String result = resolver.resolve("Hello, ${name}!", Map.of("name", "World"));
        assertThat(result).isEqualTo("Hello, World!");
    }

    @Test
    void resolve_multiplePlaceholders_allReplaced() {
        String result = resolver.resolve("${greeting}, ${name}!", Map.of("greeting", "Hi", "name", "Alice"));
        assertThat(result).isEqualTo("Hi, Alice!");
    }

    @Test
    void resolve_missingKey_replacedWithEmptyString() {
        String result = resolver.resolve("Hello, ${name}!", Map.of());
        assertThat(result).isEqualTo("Hello, !");
    }

    @Test
    void resolve_nullTemplate_returnsNull() {
        assertThat(resolver.resolve(null, Map.of("k", "v"))).isNull();
    }

    @Test
    void resolve_noPlaceholders_returnsOriginal() {
        String result = resolver.resolve("No placeholders here", Map.of("k", "v"));
        assertThat(result).isEqualTo("No placeholders here");
    }

    @Test
    void resolve_numericValue_convertedToString() {
        String result = resolver.resolve("Count: ${count}", Map.of("count", 42));
        assertThat(result).isEqualTo("Count: 42");
    }

    @Test
    void resolve_nullValue_replacedWithEmptyString() {
        String result = resolver.resolve("Val: ${val}", Collections.singletonMap("val", null));
        assertThat(result).isEqualTo("Val: ");
    }

    @Test
    void resolve_specialCharsInPlaceholderName() {
        Map<String, Object> values = Map.of("first-name", "John");
        String result = resolver.resolve("Name: ${first-name}", values);
        assertThat(result).isEqualTo("Name: John");
    }

    @Test
    void hasPlaceholders_withPlaceholder_returnsTrue() {
        assertThat(resolver.hasPlaceholders("Hello, ${name}!")).isTrue();
    }

    @Test
    void hasPlaceholders_withoutPlaceholder_returnsFalse() {
        assertThat(resolver.hasPlaceholders("No placeholders")).isFalse();
    }

    @Test
    void hasPlaceholders_nullReturnsFalse() {
        assertThat(resolver.hasPlaceholders(null)).isFalse();
    }

    @Test
    void instanceIsSingleton() {
        assertThat(PlaceholderResolver.getInstance()).isSameAs(PlaceholderResolver.getInstance());
    }
}
