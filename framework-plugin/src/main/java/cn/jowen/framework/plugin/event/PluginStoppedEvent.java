package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件停止完成事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginStoppedEvent extends PluginEvent {
    public PluginStoppedEvent(String pluginId) {
        super(pluginId, "plugin-stopped");
    }

    @Override
    public EventType getEventType() {
        return EventType.STOPPED;
    }
}
