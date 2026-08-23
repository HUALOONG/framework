package cn.jowen.framework.i18n.source;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PropertiesMessageSource} 测试。
 */
class PropertiesMessageSourceTest {

    private final PropertiesMessageSource source = new PropertiesMessageSource("i18n.messages");

    @Test
    void getMessage_existingCode_returnsMessage() {
        String result = source.getMessage("greeting", Locale.ENGLISH, null);
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    void getMessage_zhCN_returnsChineseMessage() {
        String result = source.getMessage("greeting", Locale.SIMPLIFIED_CHINESE, null);
        assertThat(result).isEqualTo("你好");
    }

    @Test
    void getMessage_withArgs_formatsMessage() {
        String result = source.getMessage("welcome", Locale.ENGLISH, new Object[]{"World"});
        assertThat(result).isEqualTo("Welcome, World");
    }

    @Test
    void getMessage_zhCN_withArgs_formatsChineseMessage() {
        String result = source.getMessage("welcome", Locale.SIMPLIFIED_CHINESE, new Object[]{"小明"});
        assertThat(result).isEqualTo("欢迎，小明");
    }

    @Test
    void getMessage_nonExistentCode_returnsNull() {
        String result = source.getMessage("nonexistent", Locale.ENGLISH, null);
        assertThat(result).isNull();
    }

    @Test
    void getMessage_nullArgs_returnsRawMessage() {
        String result = source.getMessage("welcome", Locale.ENGLISH, null);
        assertThat(result).isEqualTo("Welcome, {0}");
    }

    @Test
    void getMessage_emptyArgs_returnsRawMessage() {
        String result = source.getMessage("welcome", Locale.ENGLISH, new Object[]{});
        assertThat(result).isEqualTo("Welcome, {0}");
    }

    @Test
    void contains_existingCode_returnsTrue() {
        assertThat(source.contains("greeting", Locale.ENGLISH)).isTrue();
    }

    @Test
    void contains_nonExistentCode_returnsFalse() {
        assertThat(source.contains("nonexistent", Locale.ENGLISH)).isFalse();
    }

    @Test
    void contains_zhCN_specificMessage() {
        assertThat(source.contains("status.PAID", Locale.SIMPLIFIED_CHINESE)).isTrue();
    }

    @Test
    void reload_preservesMessages() {
        source.reload();
        String result = source.getMessage("greeting", Locale.ENGLISH, null);
        assertThat(result).isEqualTo("Hello");
    }

    @Test
    void messageCount_positive() {
        // 首次访问触发 computeIfAbsent 加载资源包
        source.getMessage("greeting", Locale.ENGLISH, null);
        source.getMessage("greeting", Locale.SIMPLIFIED_CHINESE, null);
        int count = source.messageCount();
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void getMessage_withMultipleArgs() {
        String result = source.getMessage("order.error", Locale.ENGLISH, new Object[]{"payment timeout"});
        assertThat(result).isEqualTo("Order failed: payment timeout");
    }
}
