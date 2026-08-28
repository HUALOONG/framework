package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.core.event.EventListener;
import cn.jowen.framework.i18n.api.MessageSource;
import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import cn.jowen.framework.i18n.event.ResourceLoadFailedEvent;
import cn.jowen.framework.i18n.event.ResourceReloadedEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * {@link ResourceReloader} 测试。
 */
class ResourceReloaderTest {

    @Test
    void reload_success_publishesReloadedEvent() {
        EventBus bus = new EventBus();
        List<ResourceReloadedEvent> events = new ArrayList<>();
        bus.register(new EventListener<ResourceReloadedEvent>() {
            @Override
            public void onEvent(ResourceReloadedEvent event) {
                events.add(event);
            }
        });
        ResourceReloader reloader = new ResourceReloader(bus);
        ReloadableMessageSource source = mock(ReloadableMessageSource.class);

        reloader.reload(source, Locale.US);

        verify(source).reload();
        assertThat(events).hasSize(1);
        ResourceReloadedEvent event = events.get(0);
        assertThat(event.getSource()).isSameAs(source);
        assertThat(event.getLocale()).isEqualTo(Locale.US);
        assertThat(event.getEntryCount()).isZero();
        assertThat(event.getDuration()).isNotNull();
    }

    @Test
    void reload_countableSource_reportsEntryCount() {
        EventBus bus = new EventBus();
        List<ResourceReloadedEvent> events = new ArrayList<>();
        bus.register(new EventListener<ResourceReloadedEvent>() {
            @Override
            public void onEvent(ResourceReloadedEvent event) {
                events.add(event);
            }
        });
        ResourceReloader reloader = new ResourceReloader(bus);
        ReloadableMessageSource source = new CountableSource();

        reloader.reload(source, Locale.CHINA);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEntryCount()).isEqualTo(42);
    }

    @Test
    void reload_nullLocale_usesDefault() {
        EventBus bus = new EventBus();
        List<ResourceReloadedEvent> events = new ArrayList<>();
        bus.register(new EventListener<ResourceReloadedEvent>() {
            @Override
            public void onEvent(ResourceReloadedEvent event) {
                events.add(event);
            }
        });
        ResourceReloader reloader = new ResourceReloader(bus);
        ReloadableMessageSource source = mock(ReloadableMessageSource.class);

        reloader.reload(source, (Locale) null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void reload_noEventBus_noThrow() {
        ResourceReloader reloader = new ResourceReloader();
        ReloadableMessageSource source = mock(ReloadableMessageSource.class);

        reloader.reload(source);
        reloader.reload(source, Locale.ENGLISH);
    }

    @Test
    void reload_failure_publishesFailedEvent() {
        EventBus bus = new EventBus();
        List<ResourceLoadFailedEvent> events = new ArrayList<>();
        bus.register(new EventListener<ResourceLoadFailedEvent>() {
            @Override
            public void onEvent(ResourceLoadFailedEvent event) {
                events.add(event);
            }
        });
        ResourceReloader reloader = new ResourceReloader(bus);
        ReloadableMessageSource source = mock(ReloadableMessageSource.class);
        RuntimeException failure = new IllegalStateException("boom");
        doThrow(failure).when(source).reload();

        reloader.reload(source, Locale.ENGLISH);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).getSource()).isSameAs(source);
        assertThat(events.get(0).getCause()).isSameAs(failure);
    }

    @Test
    void reload_failure_noEventBus_doesNotThrow() {
        ResourceReloader reloader = new ResourceReloader();
        ReloadableMessageSource source = mock(ReloadableMessageSource.class);
        doThrow(new IllegalStateException("boom")).when(source).reload();

        reloader.reload(source);
        reloader.reload(source, Locale.ENGLISH);
    }

    /** 同时实现计数扩展点的消息源。 */
    static final class CountableSource implements ReloadableMessageSource, ResourceReloader.CountableMessageSource {
        @Override
        public void reload() {
        }

        @Override
        public int messageCount() {
            return 42;
        }

        @Override
        public String getMessage(String code, Locale locale, Object[] args) {
            return null;
        }

        @Override
        public boolean contains(String code, Locale locale) {
            return false;
        }
    }
}
