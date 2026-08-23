package cn.jowen.framework.plugin.event;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.core.event.EventListener;
import cn.jowen.framework.plugin.DefaultPluginManager;
import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginDescriptor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PluginEventPublisherTest {

    @Test
    void eventsPublishedOnRegisterAndUnregister() {
        EventBus eventBus = new EventBus();
        PluginEventPublisher publisher = new PluginEventPublisher(eventBus);
        List<String> received = new ArrayList<>();

        publisher.subscribe(new EventListener<PluginLifecycleEvent>() {
            @Override
            public void onEvent(PluginLifecycleEvent event) {
                received.add(event.getClass().getSimpleName());
            }
        });

        DefaultPluginManager manager = new DefaultPluginManager(eventBus);
        manager.register(makePlugin("p1"));
        manager.unregister("p1");

        assertThat(received).contains(
                "PluginInstallEvent", "PluginLoadedEvent", "PluginStartedEvent",
                "PluginStoppedEvent", "PluginUnloadedEvent"
        );
    }

    private static Plugin makePlugin(String id) {
        return new Plugin() {
            @Override public String id() { return id; }
            @Override public String version() { return "1.0.0"; }
            @Override public void afterPropertiesSet() {}
            @Override public void destroy() {}
            @Override public PluginDescriptor descriptor() {
                return PluginDescriptor.of(id, "1.0.0", "cn.jowen.framework.plugin.event.PluginEventPublisherTest$TestImpl");
            }
        };
    }
}
