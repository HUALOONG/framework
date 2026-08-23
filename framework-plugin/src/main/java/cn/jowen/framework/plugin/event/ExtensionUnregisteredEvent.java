package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

/**
 * 扩展注销事件。
 *
 * @author 王飞
 */
@NullMarked
public final class ExtensionUnregisteredEvent extends PluginEvent {
    private final String extensionId;
    private final String extensionPointId;

    public ExtensionUnregisteredEvent(String pluginId, String extensionId, String extensionPointId) {
        super(pluginId, "extension-unregistered");
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
        return EventType.EXTENSION_UNREGISTERED;
    }
}
