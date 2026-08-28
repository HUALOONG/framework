package cn.jowen.framework.i18n.event;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ResourceReloadedEvent} 测试。
 */
class ResourceReloadedEventTest {

    @Test
    void event_holdsAllFields() {
        Object source = new Object();
        ResourceReloadedEvent event =
                new ResourceReloadedEvent(source, Locale.US, 42, Duration.ofMillis(5));

        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getLocale()).isEqualTo(Locale.US);
        assertThat(event.getEntryCount()).isEqualTo(42);
        assertThat(event.getDuration()).isEqualTo(Duration.ofMillis(5));
    }

    @Test
    void toString_includesFields() {
        ResourceReloadedEvent event =
                new ResourceReloadedEvent("source", Locale.US, 42, Duration.ofMillis(5));

        String text = event.toString();

        assertThat(text).contains("locale=en_US").contains("entryCount=42").contains("duration=PT0.005S");
    }
}
