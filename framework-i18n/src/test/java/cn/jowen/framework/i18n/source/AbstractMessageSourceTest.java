package cn.jowen.framework.i18n.source;

import cn.jowen.framework.i18n.format.JavaTextMessageFormatter;
import cn.jowen.framework.i18n.format.MessageFormatter;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AbstractMessageSource} 测试：区域回退链与格式化器切换。
 */
class AbstractMessageSourceTest {

    private final StubSource source = new StubSource();

    @Test
    void getMessage_exactLocale_hit() {
        source.put("greeting", Locale.CHINA, "你好");
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("你好");
    }

    @Test
    void getMessage_languageFallback() {
        source.put("greeting", Locale.of("zh"), "语言级");
        // zh_CN 未命中 → 回退语言 zh
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("语言级");
    }

    @Test
    void getMessage_rootFallback() {
        source.put("greeting", Locale.ROOT, "默认");
        assertThat(source.getMessage("greeting", Locale.US, null)).isEqualTo("默认");
    }

    @Test
    void getMessage_prefersExactOverLanguage() {
        source.put("greeting", Locale.CHINA, "精确");
        source.put("greeting", Locale.of("zh"), "语言级");
        assertThat(source.getMessage("greeting", Locale.CHINA, null)).isEqualTo("精确");
    }

    @Test
    void getMessage_missing_returnsNull() {
        assertThat(source.getMessage("nope", Locale.US, null)).isNull();
    }

    @Test
    void contains_checksAnyLevel() {
        source.put("greeting", Locale.ROOT, "默认");
        assertThat(source.contains("greeting", Locale.US)).isTrue();
        assertThat(source.contains("nope", Locale.US)).isFalse();
    }

    @Test
    void defaultFormatter_javaText() {
        source.put("welcome", Locale.ROOT, "Welcome, {0}");
        assertThat(source.getMessage("welcome", Locale.US, new Object[]{"Tom"})).isEqualTo("Welcome, Tom");
    }

    @Test
    void setFormatter_object_overrides() {
        source.put("welcome", Locale.ROOT, "Welcome, {0}");
        source.setFormatter((MessageFormatter) JavaTextMessageFormatter.getInstance());
        assertThat(source.getMessage("welcome", Locale.US, new Object[]{"Tom"})).isEqualTo("Welcome, Tom");
    }

    @Test
    void setFormatter_byName_namedParameter() {
        source.put("welcome", Locale.ROOT, "Welcome, {name}");
        source.setFormatter("named-parameter");
        assertThat(source.getMessage("welcome", Locale.US, new Object[]{Map.of("name", "Tom")}))
                .isEqualTo("Welcome, Tom");
    }

    @Test
    void setFormatter_unknownName_throws() {
        assertThatThrownBy(() -> source.setFormatter("not-exist"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void setFormatter_null_throws() {
        assertThatThrownBy(() -> source.setFormatter((MessageFormatter) null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void reload_default_noop() {
        source.reload();
        source.put("a", Locale.ROOT, "1");
        assertThat(source.getMessage("a", Locale.ROOT, null)).isEqualTo("1");
    }

    private static final class StubSource extends AbstractMessageSource {
        private final Map<String, String> messages = new java.util.HashMap<>();

        void put(String code, Locale locale, String raw) {
            messages.put(key(code, locale), raw);
        }

        @Override
        protected @Nullable String loadRaw(String code, Locale locale) {
            return messages.get(key(code, locale));
        }

        private static String key(String code, Locale locale) {
            return code + "|" + locale.toLanguageTag();
        }
    }
}
