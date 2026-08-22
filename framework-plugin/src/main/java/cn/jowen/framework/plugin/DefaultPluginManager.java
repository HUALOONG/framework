package cn.jowen.framework.plugin;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认插件管理器。维护注册表并按注册顺序编排生命周期，支持按隔离类加载器热加载插件。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class DefaultPluginManager implements PluginManager {

    /** 注册表：id → 插件（保持插入顺序，便于逆序停止）。 */
    private final Map<String, Plugin> registry = new LinkedHashMap<>();

    @Override
    public void register(Plugin plugin) {
        String id = plugin.id();
        if (registry.containsKey(id)) {
            throw new PluginLoader.PluginException("插件 id 已存在：" + id);
        }
        try {
            plugin.afterPropertiesSet();
        } catch (Exception e) {
            throw new PluginLoader.PluginException("插件启动失败：" + id, e);
        }
        registry.put(id, plugin);
    }

    @Override
    public void load(PluginLoader loader) {
        Plugin plugin = loader.load();
        register(plugin);
    }

    @Override
    public void unregister(String id) {
        Plugin plugin = registry.remove(id);
        if (plugin != null) {
            try {
                plugin.destroy();
            } catch (Exception e) {
                throw new PluginLoader.PluginException("插件停止失败：" + id, e);
            }
        }
    }

    @Override
    public void stopAll() {
        // 逆序停止，尊重依赖方向
        List<Plugin> reversed = new ArrayList<>(registry.values());
        java.util.Collections.reverse(reversed);
        for (Plugin plugin : reversed) {
            try {
                plugin.destroy();
            } catch (Exception ignored) {
                // 停止异常不应阻断其余插件
            }
        }
        registry.clear();
    }

    @Override
    public @Nullable Plugin get(String id) {
        return registry.get(id);
    }

    @Override
    public List<Plugin> all() {
        return new ArrayList<>(registry.values());
    }
}
