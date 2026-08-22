package cn.jowen.framework.plugin.event;

import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * 插件停止事件。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginStoppedEvent extends PluginLifecycleEvent {

    public PluginStoppedEvent(Plugin plugin) {
        super(plugin);
    }
}
