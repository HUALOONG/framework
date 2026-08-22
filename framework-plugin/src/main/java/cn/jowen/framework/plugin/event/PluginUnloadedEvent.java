package cn.jowen.framework.plugin.event;

import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * 插件卸载事件，在插件从注册表移除时发布。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginUnloadedEvent extends PluginLifecycleEvent {

    public PluginUnloadedEvent(Plugin plugin) {
        super(plugin);
    }
}
