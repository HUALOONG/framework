package cn.jowen.framework.i18n;

import cn.jowen.framework.i18n.api.LocaleContextHolder;
import cn.jowen.framework.i18n.source.CompositeMessageSource;
import cn.jowen.framework.i18n.source.PropertiesMessageSource;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class I18nTest {

    @Test
    void resolvesByLocaleWithFallback() {
        PropertiesMessageSource source = new PropertiesMessageSource("i18n/messages");
        assertThat(source.getMessage("greeting", Locale.ENGLISH, null)).isEqualTo("Hello");
        assertThat(source.getMessage("greeting", new Locale("zh", "CN"), null)).isEqualTo("你好");
        // 默认回退（messages.properties 未定义 fallback.key，应返回 null）
        assertThat(source.getMessage("undefined", Locale.ENGLISH, null)).isNull();
    }

    @Test
    void formatsWithArgs() {
        PropertiesMessageSource source = new PropertiesMessageSource("i18n/messages");
        String msg = source.getMessage("welcome", Locale.ENGLISH, new Object[]{"Jowen"});
        assertThat(msg).isEqualTo("Welcome, Jowen");
    }

    @Test
    void localeContextHolderPropagates() {
        LocaleContextHolder.setLocale(new Locale("zh", "CN"));
        try {
            PropertiesMessageSource source = new PropertiesMessageSource("i18n/messages");
            assertThat(source.getMessage("greeting", null)).isEqualTo("你好");
        } finally {
            LocaleContextHolder.resetLocale();
        }
    }

    @Test
    void compositeAggregatesSources() {
        CompositeMessageSource composite = new CompositeMessageSource();
        composite.add(new PropertiesMessageSource("i18n/messages"));
        composite.add(new PropertiesMessageSource("i18n/extra"));
        assertThat(composite.getMessage("greeting", Locale.ENGLISH, null)).isEqualTo("Hello");
        assertThat(composite.getMessage("extra.only", Locale.ENGLISH, null)).isEqualTo("Extra message");
    }
}
