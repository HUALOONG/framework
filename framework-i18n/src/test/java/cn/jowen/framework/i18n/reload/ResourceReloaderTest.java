package cn.jowen.framework.i18n.reload;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.i18n.api.ReloadableMessageSource;
import cn.jowen.framework.i18n.event.I18nEventListener;
import cn.jowen.framework.i18n.event.ResourceLoadFailedEvent;
import cn.jowen.framework.i18n.event.ResourceReloadedEvent;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceReloaderTest {

    /** 计数型消息源：每次 reload 递增计数。 */
    static class CountingSource implements ReloadableMessageSource {
        final AtomicInteger reloads = new AtomicInteger();

        @Override
        public String getMessage(String code, Locale locale, Object[] args) {
            return code;
        }

        @Override
        public boolean contains(String code, Locale locale) {
            return true;
        }

        @Override
        public void reload() {
            reloads.incrementAndGet();
        }
    }

    /** 抛错消息源：模拟重载失败。 */
    static class FailingSource implements ReloadableMessageSource {
        @Override
        public String getMessage(String code, Locale locale, Object[] args) {
            return code;
        }

        @Override
        public boolean contains(String code, Locale locale) {
            return true;
        }

        @Override
        public void reload() {
            throw new IllegalStateException("boom");
        }
    }

    @Test
    void publishesReloadedEventOnSuccess() {
        EventBus bus = new EventBus();
        CountingSource source = new CountingSource();
        ResourceReloader reloader = new ResourceReloader(bus);

        AtomicReference<ResourceReloadedEvent> received = new AtomicReference<>();
        // 注意：匿名类保留泛型参数，lambda 无法被 EventBus 推断事件类型
        bus.register(new I18nEventListener<ResourceReloadedEvent>() {
            @Override
            public void onEvent(ResourceReloadedEvent event) {
                received.set(event);
            }
        });

        reloader.reload(source);

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getSource()).isSameAs(source);
        assertThat(source.reloads).hasValue(1);
    }

    @Test
    void publishesLoadFailedEventOnError() {
        EventBus bus = new EventBus();
        FailingSource source = new FailingSource();
        ResourceReloader reloader = new ResourceReloader(bus);

        AtomicReference<ResourceLoadFailedEvent> received = new AtomicReference<>();
        bus.register(new I18nEventListener<ResourceLoadFailedEvent>() {
            @Override
            public void onEvent(ResourceLoadFailedEvent event) {
                received.set(event);
            }
        });

        reloader.reload(source);

        assertThat(received.get()).isNotNull();
        assertThat(received.get().getCause()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runsWithoutEventBus() {
        CountingSource source = new CountingSource();
        ResourceReloader reloader = new ResourceReloader();
        reloader.reload(source);
        assertThat(source.reloads).hasValue(1);
    }
}
