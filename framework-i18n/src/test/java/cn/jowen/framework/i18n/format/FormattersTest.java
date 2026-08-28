package cn.jowen.framework.i18n.format;

import cn.jowen.framework.i18n.api.FormatException;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JavaTextMessageFormatter}、{@link NamedParameterMessageFormatter}、
 * {@link IcuMessageFormatter}、{@link FormatterRegistry} 测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class FormattersTest {

    // ===================== JavaTextMessageFormatter =====================

    @Test
    void javaText_name() {
        assertThat(JavaTextMessageFormatter.getInstance().name()).isEqualTo(JavaTextMessageFormatter.NAME);
    }

    @Test
    void javaText_positional() {
        String result = JavaTextMessageFormatter.getInstance()
                .format("hello {0}, you are {1}", new Object[]{"tom", 18}, Locale.ENGLISH);
        assertThat(result).isEqualTo("hello tom, you are 18");
    }

    @Test
    void javaText_numberSubformat() {
        String result = JavaTextMessageFormatter.getInstance()
                .format("price {0,number,#.##}", new Object[]{12.3}, Locale.ENGLISH);
        assertThat(result).isEqualTo("price 12.3");
    }

    @Test
    void javaText_nullArgs() {
        String result = JavaTextMessageFormatter.getInstance().format("plain", null, Locale.ENGLISH);
        assertThat(result).isEqualTo("plain");
    }

    @Test
    void javaText_badPattern_throws() {
        assertThatThrownBy(() -> JavaTextMessageFormatter.getInstance()
                .format("{0,number,", new Object[]{1}, Locale.ENGLISH))
                .isInstanceOf(FormatException.class);
    }

    // ===================== NamedParameterMessageFormatter =====================

    @Test
    void named_name() {
        assertThat(NamedParameterMessageFormatter.getInstance().name()).isEqualTo(NamedParameterMessageFormatter.NAME);
    }

    @Test
    void named_resolvesByKey() {
        String result = NamedParameterMessageFormatter.getInstance()
                .format("hello {name}, count={count}", new Object[]{Map.of("name", "tom", "count", 3)}, Locale.ENGLISH);
        assertThat(result).isEqualTo("hello tom, count=3");
    }

    @Test
    void named_missingKey_resolvesToEmpty() {
        String result = NamedParameterMessageFormatter.getInstance()
                .format("hello {name}", new Object[]{Map.of("other", "x")}, Locale.ENGLISH);
        assertThat(result).isEqualTo("hello ");
    }

    @Test
    void named_nullArgs() {
        String result = NamedParameterMessageFormatter.getInstance().format("plain", null, Locale.ENGLISH);
        assertThat(result).isEqualTo("plain");
    }

    @Test
    void named_unclosedPlaceholder_throws() {
        assertThatThrownBy(() -> NamedParameterMessageFormatter.getInstance()
                .format("x {a", new Object[]{Map.of("a", "1")}, Locale.ENGLISH))
                .isInstanceOf(FormatException.class);
    }

    @Test
    void named_unsupportedArgType_throws() {
        assertThatThrownBy(() -> NamedParameterMessageFormatter.getInstance()
                .format("x {a}", new Object[]{"not a map"}, Locale.ENGLISH))
                .isInstanceOf(FormatException.class);
    }

    // ===================== IcuMessageFormatter（ICU4J 为可选依赖） =====================

    @Test
    void icu_name() {
        assertThat(IcuMessageFormatter.getInstance().name()).isEqualTo(IcuMessageFormatter.NAME);
    }

    @Test
    void icu_unavailableWhenNoDependency() {
        // 当前 classpath 未引入 com.ibm.icu:icu4j
        assertThat(IcuMessageFormatter.available()).isFalse();
    }

    @Test
    void icu_formatThrowsWithoutDependency() {
        assertThatThrownBy(() -> IcuMessageFormatter.getInstance()
                .format("hello {name}", new Object[]{"tom"}, Locale.ENGLISH))
                .isInstanceOf(FormatException.class)
                .hasMessageContaining("ICU4J");
    }

    // ===================== FormatterRegistry =====================

    @Test
    void registry_defaultFormattersRegistered() {
        FormatterRegistry registry = new FormatterRegistry();
        assertThat(registry.get(JavaTextMessageFormatter.NAME)).isNotNull();
        assertThat(registry.get(NamedParameterMessageFormatter.NAME)).isNotNull();
        assertThat(registry.contains(JavaTextMessageFormatter.NAME)).isTrue();
    }

    @Test
    void registry_getUnknown_returnsNull() {
        FormatterRegistry registry = new FormatterRegistry();
        assertThat(registry.get("nope")).isNull();
        assertThat(registry.contains("nope")).isFalse();
    }

    @Test
    void registry_registerOverrideAndChain() {
        FormatterRegistry registry = new FormatterRegistry();
        IcuMessageFormatter icu = IcuMessageFormatter.getInstance();
        FormatterRegistry returned = registry.register(icu);
        assertThat(returned).isSameAs(registry);
        assertThat(registry.get(IcuMessageFormatter.NAME)).isSameAs(icu);
    }

    @Test
    void registry_allReturnsRegistered() {
        FormatterRegistry registry = new FormatterRegistry();
        Map<String, MessageFormatter> all = registry.all();
        assertThat(all).containsKeys(JavaTextMessageFormatter.NAME, NamedParameterMessageFormatter.NAME);
    }

    @Test
    void registry_defaultFormatterConstant() {
        assertThat(FormatterRegistry.DEFAULT_FORMATTER).isEqualTo(JavaTextMessageFormatter.NAME);
    }
}
