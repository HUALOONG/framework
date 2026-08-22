package cn.jowen.framework.plugin.event;

import cn.jowen.framework.plugin.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

/**
 * 插件安装事件，在插件描述符提交注册时发布（早于实际加载）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginInstallEvent extends PluginLifecycleEvent {

    private final PluginDescriptor descriptor;

    public PluginInstallEvent(cn.jowen.framework.plugin.Plugin plugin, PluginDescriptor descriptor) {
        super(plugin);
        this.descriptor = descriptor;
    }

    /** @return 关联的描述符，不可为 {@code null} */
    public PluginDescriptor getDescriptor() {
        return descriptor;
    }
}
