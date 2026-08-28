package cn.jowen.framework.i18n.source;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ResourceBundleMessageSource} 测试（基于测试资源 {@code i18n/messages*.properties}）。
 */
class ResourceBundleMessageSourceTest {

    private final ResourceBundleMessageSource source = new ResourceBundleMessageSource("i18n.messages");

    @Test
    void load_exactLocaleHit() {
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("你好");
        assertThat(source.contains("greeting", Locale.CHINA)).isTrue();
    }

    @Test
    void load_languageFallback() {
        // 无 messages_zh.properties，zh_CN 请求应命中 zh_CN 包
        assertThat(source.getMessage("greeting", Locale.of("zh", "CN"), null)).isEqualTo("你好");
    }

    @Test
    void load_rootFallback() {
        assertThat(source.getMessage("greeting", Locale.US, null)).isEqualTo("Hello");
        assertThat(source.getMessage("greeting", Locale.ROOT, null)).isEqualTo("Hello");
    }

    @Test
    void load_missing_returnsNull() {
        assertThat(source.getMessage("nope", Locale.US, null)).isNull();
        assertThat(source.contains("nope", Locale.US)).isFalse();
    }

    @Test
    void load_unknownLocale_fallsBackToRoot() {
        // 候选链 [fr_FR, fr, ROOT]，fr 无包时回退到根资源
        assertThat(source.getMessage("greeting", Locale.FRANCE, null)).isEqualTo("Hello");
    }

    @Test
    void format_appliesArguments() {
        assertThat(source.getMessage("welcome", Locale.CHINA, new Object[]{"张三"})).isEqualTo("欢迎，张三");
        assertThat(source.getMessage("order.error", Locale.US, new Object[]{"E001"})).isEqualTo("Order failed: E001");
    }

    @Test
    void reload_doesNotThrow() {
        source.reload();
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("你好");
    }

    @Test
    void setFormatterByName_namedParameter() {
        source.setFormatter("named-parameter");
        assertThat(source.getMessage("welcome", Locale.US, new Object[]{java.util.Map.of("0", "Tom")}))
                .isEqualTo("Welcome, Tom");
    }

    @Test
    void setFormatterByName_unknown_throws() {
        assertThatThrownBy(() -> source.setFormatter("unknown"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
