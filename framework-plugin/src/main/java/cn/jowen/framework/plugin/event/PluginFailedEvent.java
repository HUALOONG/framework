package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 插件启动/停止失败事件。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginFailedEvent extends PluginEvent {
    private final String errorMessage;

    public PluginFailedEvent(String pluginId, String errorMessage) {
        super(pluginId, "plugin-failed");
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public EventType getEventType() {
        return EventType.FAILED;
    }
}
