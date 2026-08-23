package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件启动完成事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginStartedEvent extends PluginEvent {
    public PluginStartedEvent(String pluginId) {
        super(pluginId, "plugin-started");
    }

    @Override
    public EventType getEventType() {
        return EventType.STARTED;
    }
}
