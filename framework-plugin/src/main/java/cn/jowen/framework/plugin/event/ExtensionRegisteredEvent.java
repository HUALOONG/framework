package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 扩展注册事件。
 *
 * @author 王飞
 */
@NullMarked
public final class ExtensionRegisteredEvent extends PluginEvent {
    private final String extensionId;
    private final String extensionPointId;

    public ExtensionRegisteredEvent(String pluginId, String extensionId, String extensionPointId) {
        super(pluginId, "extension-registered");
        this.extensionId = extensionId;
        this.extensionPointId = extensionPointId;
    }

    public String getExtensionId() {
        return extensionId;
    }

    public String getExtensionPointId() {
        return extensionPointId;
    }

    @Override
    public EventType getEventType() {
        return EventType.EXTENSION_REGISTERED;
    }
}
