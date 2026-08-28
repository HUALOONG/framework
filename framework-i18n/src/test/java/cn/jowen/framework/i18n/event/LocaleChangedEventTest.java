package cn.jowen.framework.i18n.event;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocaleChangedEvent} 测试。
 */
class LocaleChangedEventTest {

    @Test
    void event_holdsPreviousAndCurrentLocale() {
        Locale previous = Locale.US;
        Locale current = Locale.SIMPLIFIED_CHINESE;
        Object source = new Object();

        LocaleChangedEvent event = new LocaleChangedEvent(source, previous, current);

        assertThat(event.getPrevious()).isEqualTo(previous);
        assertThat(event.getCurrent()).isEqualTo(current);
        assertThat(event.getSource()).isSameAs(source);
    }

    @Test
    void toString_includesLocales() {
        LocaleChangedEvent event = new LocaleChangedEvent(null, Locale.US, Locale.SIMPLIFIED_CHINESE);

        String text = event.toString();

        assertThat(text).contains("en_US").contains("zh_CN");
    }
}
