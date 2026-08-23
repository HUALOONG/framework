package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件卸载事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginUnloadedEvent extends PluginEvent {
    public PluginUnloadedEvent(String pluginId) {
        super(pluginId, "plugin-unloaded");
    }

    @Override
    public EventType getEventType() {
        return EventType.UNLOADED;
    }
}
