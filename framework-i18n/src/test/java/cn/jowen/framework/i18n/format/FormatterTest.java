package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FormatterTest {

    @Test
    void javaTextFormatsPositionalArgs() {
        MessageFormatter formatter = JavaTextMessageFormatter.getInstance();
        String result = formatter.format("Hello, {0}!", new Object[]{"Jowen"}, Locale.ENGLISH);
        assertThat(result).isEqualTo("Hello, Jowen!");
    }

    @Test
    void javaTextSupportsChoiceAndPlural() {
        MessageFormatter formatter = JavaTextMessageFormatter.getInstance();
        String result = formatter.format("{0,choice,0#no items|1#one item|1<{0} items}",
                new Object[]{3}, Locale.ENGLISH);
        assertThat(result).isEqualTo("3 items");
    }

    @Test
    void javaTextThrowsFormatExceptionOnBadPattern() {
        MessageFormatter formatter = JavaTextMessageFormatter.getInstance();
        assertThatThrownBy(() -> formatter.format("Hello {0", new Object[]{"x"}, Locale.ENGLISH))
                .isInstanceOf(FormatException.class);
    }

    @Test
    void namedParameterFormatsMapArgs() {
        MessageFormatter formatter = NamedParameterMessageFormatter.getInstance();
        String result = formatter.format("Hi {name}, you are {age}",
                new Object[]{Map.of("name", "Jowen", "age", 18)}, Locale.ENGLISH);
        assertThat(result).isEqualTo("Hi Jowen, you are 18");
    }

    @Test
    void namedParameterMissingKeyBecomesEmpty() {
        MessageFormatter formatter = NamedParameterMessageFormatter.getInstance();
        String result = formatter.format("Hi {name}", new Object[]{Map.of()}, Locale.ENGLISH);
        assertThat(result).isEqualTo("Hi ");
    }

    @Test
    void namedParameterRejectsNonMapArgs() {
        MessageFormatter formatter = NamedParameterMessageFormatter.getInstance();
        assertThatThrownBy(() -> formatter.format("Hi {name}", new Object[]{"x"}, Locale.ENGLISH))
                .isInstanceOf(FormatException.class);
    }

    @Test
    void registryPreloadsDefaultsAndAllowsOverride() {
        FormatterRegistry registry = new FormatterRegistry();
        assertThat(registry.get("java-text")).isSameAs(JavaTextMessageFormatter.getInstance());
        assertThat(registry.get("named-parameter")).isSameAs(NamedParameterMessageFormatter.getInstance());
        assertThat(registry.contains("icu")).isFalse();
        registry.register(IcuMessageFormatter.getInstance());
        assertThat(registry.contains("icu")).isTrue();
        assertThat(registry.all()).hasSize(3);
    }
}
