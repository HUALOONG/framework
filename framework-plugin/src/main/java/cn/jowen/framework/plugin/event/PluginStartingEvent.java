package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件启动中事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginStartingEvent extends PluginEvent {
    public PluginStartingEvent(String pluginId) {
        super(pluginId, "plugin-starting");
    }

    @Override
    public EventType getEventType() {
        return EventType.STARTING;
    }
}
