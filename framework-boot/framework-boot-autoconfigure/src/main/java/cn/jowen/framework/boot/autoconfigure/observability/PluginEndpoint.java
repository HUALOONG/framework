package cn.jowen.framework.boot.autoconfigure.observability;

import cn.jowen.framework.plugin.Plugin;
import cn.jowen.framework.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;

import java.util.List;

/**
 * 插件可观测端点：暴露已注册插件清单与单个插件状态（{@code /actuator/frameworkPlugins}）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Endpoint(id = "frameworkPlugins")
public class PluginEndpoint {

    private final PluginManager pluginManager;

    public PluginEndpoint(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /** @return 全部已注册插件（id + 版本）。 */
    @ReadOperation
    public List<PluginInfo> plugins() {
        return pluginManager.all().stream().map(PluginInfo::from).toList();
    }

    /** @param id 插件 id，不存在返回 {@code null} */
    @ReadOperation
    public @Nullable PluginInfo plugin(@Selector String id) {
        Plugin plugin = pluginManager.get(id);
        return plugin == null ? null : PluginInfo.from(plugin);
    }

    /** 插件信息投影。 */
    public record PluginInfo(String id, String version) {

        static PluginInfo from(Plugin plugin) {
            return new PluginInfo(plugin.id(), plugin.version());
        }
    }
}
