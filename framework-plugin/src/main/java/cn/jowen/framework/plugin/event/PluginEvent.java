package cn.jowen.framework.plugin.event;

import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * 插件事件基类。
 *
 * @param pluginId  相关插件 id
 * @param timestamp 事件时间戳
 * @param source    事件来源
 * @author 王飞
 */
@NullMarked
public abstract class PluginEvent {

    private final String pluginId;
    private final Instant timestamp;
    private final String source;

    protected PluginEvent(String pluginId, String source) {
        this.pluginId = pluginId;
        this.timestamp = Instant.now();
        this.source = source;
    }

    public String getPluginId() {
        return pluginId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }

    public abstract EventType getEventType();

    public enum EventType {
        LOADED, STARTING, STARTED, STOPPING, STOPPED, FAILED, UNLOADED,
        EXTENSION_REGISTERED, EXTENSION_UNREGISTERED
    }
}
