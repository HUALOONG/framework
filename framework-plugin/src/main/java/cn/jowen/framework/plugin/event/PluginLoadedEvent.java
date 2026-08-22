package cn.jowen.framework.plugin.event;

import cn.jowen.framework.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

/**
 * 插件加载完成事件，在 {@code PluginManager#register} 成功插入后发布。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginLoadedEvent extends PluginLifecycleEvent {

    public PluginLoadedEvent(Plugin plugin) {
        super(plugin);
    }
}
