package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件停止中事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginStoppingEvent extends PluginEvent {
    public PluginStoppingEvent(String pluginId) {
        super(pluginId, "plugin-stopping");
    }

    @Override
    public EventType getEventType() {
        return EventType.STOPPING;
    }
}
