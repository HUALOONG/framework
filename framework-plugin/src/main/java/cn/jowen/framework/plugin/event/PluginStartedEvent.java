package cn.jowen.framework.plugin.event;

import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * 插件启动完成事件。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginStartedEvent extends PluginLifecycleEvent {

    public PluginStartedEvent(Plugin plugin) {
        super(plugin);
    }
}
