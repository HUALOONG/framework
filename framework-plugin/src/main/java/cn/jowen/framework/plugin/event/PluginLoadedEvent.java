package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件加载完成事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginLoadedEvent extends PluginEvent {
    public PluginLoadedEvent(String pluginId) {
        super(pluginId, "plugin-loaded");
    }

    @Override
    public EventType getEventType() {
        return EventType.LOADED;
    }
}
