package cn.jowen.framework.plugin.event;

import cn.jowen.framework.core.event.FrameworkEvent;
import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * 插件事件基类，承载插件标识与插件实例。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public abstract class PluginLifecycleEvent extends FrameworkEvent {

    private final Plugin plugin;

    protected PluginLifecycleEvent(Plugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    /** @return 事件关联的插件，不可为 {@code null} */
    public Plugin getPlugin() {
        return plugin;
    }

    /** @return 插件 id */
    public String pluginId() {
        return plugin.id();
    }

    /** @return 插件版本 */
    public String pluginVersion() {
        return plugin.version();
    }
}
