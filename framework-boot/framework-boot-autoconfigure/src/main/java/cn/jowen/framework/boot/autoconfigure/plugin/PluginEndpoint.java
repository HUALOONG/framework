package cn.jowen.framework.boot.autoconfigure.plugin;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

import java.util.ArrayList;
import java.util.List;

/**
 * Actuator 插件管理端点 {@code /actuator/plugins}：查询、启动、停止、重启与卸载插件。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
@Endpoint(id = "plugins")
public class PluginEndpoint {

    /**
     * 插件状态摘要。
     */
    public record PluginStatus(String pluginId, @Nullable PluginState state) {
    }

    private final PluginManager pluginManager;

    /**
     * 构造端点。
     *
     * @param pluginManager 插件管理器，不可为 {@code null}
     */
    public PluginEndpoint(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * 查询全部插件状态。
     *
     * @return 插件状态列表
     */
    @ReadOperation
    public List<PluginStatus> listPlugins() {
        List<PluginStatus> result = new ArrayList<>();
        for (Plugin plugin : pluginManager.getPlugins()) {
            result.add(new PluginStatus(plugin.getDescriptor().pluginId(), plugin.getState()));
        }
        return result;
    }

    /**
     * 启动插件。
     *
     * @param pluginId 插件 ID
     * @return 启动后的插件状态
     */
    @WriteOperation
    public PluginStatus start(@Selector String pluginId) {
        pluginManager.startPlugin(pluginId);
        return status(pluginId);
    }

    /**
     * 停止插件。
     *
     * @param pluginId 插件 ID
     * @return 停止后的插件状态
     */
    @WriteOperation
    public PluginStatus stop(@Selector String pluginId) {
        pluginManager.stopPlugin(pluginId);
        return status(pluginId);
    }

    /**
     * 重启插件。
     *
     * @param pluginId 插件 ID
     * @return 重启后的插件状态
     */
    @WriteOperation
    public PluginStatus restart(@Selector String pluginId) {
        pluginManager.restartPlugin(pluginId);
        return status(pluginId);
    }

    /**
     * 卸载插件。
     *
     * @param pluginId 插件 ID
     */
    @DeleteOperation
    public void unload(@Selector String pluginId) {
        pluginManager.unloadPlugin(pluginId);
    }

    private PluginStatus status(String pluginId) {
        Plugin plugin = pluginManager.getPlugin(pluginId);
        return new PluginStatus(pluginId, plugin == null ? null : plugin.getState());
    }
}