package cn.jowen.framework.plugin.event;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.core.event.EventListener;
import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginDescriptor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 插件事件发布器。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginEventPublisher {

    private final EventBus eventBus;

    public PluginEventPublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    public EventBus getEventBus() { return eventBus; }

    public void publishPluginLoaded(Plugin plugin) {
        eventBus.publish(new PluginLoadedEvent(plugin));
    }

    public void publishPluginStarted(Plugin plugin) {
        eventBus.publish(new PluginStartedEvent(plugin));
    }

    public void publishPluginStopped(Plugin plugin) {
        eventBus.publish(new PluginStoppedEvent(plugin));
    }

    public void publishPluginUnloaded(Plugin plugin) {
        eventBus.publish(new PluginUnloadedEvent(plugin));
    }

    public void publishPluginInstall(Plugin plugin, PluginDescriptor descriptor) {
        eventBus.publish(new PluginInstallEvent(plugin, descriptor));
    }

    public void subscribe(EventListener<PluginLifecycleEvent> listener) {
        eventBus.register(listener);
    }
}
