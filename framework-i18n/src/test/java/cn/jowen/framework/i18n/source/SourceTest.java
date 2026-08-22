package cn.jowen.framework.i18n.source;

import cn.jowen.framework.i18n.api.MessageNotFoundException;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.i18n.api.MessageSourceResolvable;
import cn.jowen.framework.i18n.format.NamedParameterMessageFormatter;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SourceTest {

    @Test
    void resourceBundleSourceResolvesWithJdkFallback() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource("i18n.messages");
        assertThat(source.getMessage("greeting", Locale.ENGLISH, null)).isEqualTo("Hello");
        assertThat(source.getMessage("greeting", new Locale("zh", "CN"), null)).isEqualTo("你好");
        assertThat(source.getMessage("missing", Locale.ENGLISH, null)).isNull();
    }

    @Test
    void abstractSourceSupportsNamedParameterFormatter() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource("i18n.messages");
        source.setFormatter(NamedParameterMessageFormatter.getInstance());
        String msg = source.getMessage("welcome", Locale.ENGLISH, new Object[]{Map.of("0", "Jowen")});
        // {0} 在命名参数模式下被当作键名 "0"
        assertThat(msg).isEqualTo("Welcome, Jowen");
    }

    @Test
    void messageSourceResolvableFallsBackToDefault() {
        MessageSource source = new ResourceBundleMessageSource("i18n.messages");
        String msg = source.getMessage(new MessageSourceResolvable("missing.key", "fallback text"), Locale.ENGLISH);
        assertThat(msg).isEqualTo("fallback text");
        assertThat(source.getMessage(new MessageSourceResolvable("greeting", null), Locale.ENGLISH))
                .isEqualTo("Hello");
    }

    @Test
    void getMessageRequiredThrowsWhenMissing() {
        MessageSource source = new ResourceBundleMessageSource("i18n.messages");
        assertThatThrownBy(() -> source.getMessageRequired("missing.key", Locale.ENGLISH, null))
                .isInstanceOf(MessageNotFoundException.class);
    }
}
