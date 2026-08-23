package cn.jowen.framework.plugin.registry;

import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 插件注册中心。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginRegistry {

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();

    /**
     * 注册插件（唯一性校验）。
     *
     * @param plugin 插件实例，不可为 {@code null}
     * @throws IllegalStateException 插件 id 已存在时抛出
     */
    public void register(Plugin plugin) {
        if (plugin == null) throw new IllegalArgumentException("plugin cannot be null");
        String id = plugin.getDescriptor().pluginId();
        if (plugins.containsKey(id)) {
            throw new IllegalStateException("插件 id 已存在：" + id);
        }
        plugins.put(id, plugin);
    }

    /**
     * 注销插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     * @return 被注销的插件，不存在时返回 {@code null}
     */
    public @Nullable Plugin unregister(String pluginId) {
        return plugins.remove(pluginId);
    }

    /**
     * 获取插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     * @return 插件实例，不存在时返回 {@code null}
     */
    public @Nullable Plugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    /**
     * 获取所有插件。
     */
    public List<Plugin> getPlugins() {
        return new ArrayList<>(plugins.values());
    }

    /**
     * 按状态筛选插件。
     *
     * @param state 目标状态
     * @return 匹配状态的插件列表
     */
    public List<Plugin> getPluginsByState(PluginState state) {
        List<Plugin> result = new ArrayList<>();
        for (Plugin p : plugins.values()) {
            if (p.getState() == state) result.add(p);
        }
        return result;
    }

    /**
     * 插件数量。
     */
    public int size() {
        return plugins.size();
    }

    /**
     * 清空注册表。
     */
    public void clear() {
        plugins.clear();
    }
}
