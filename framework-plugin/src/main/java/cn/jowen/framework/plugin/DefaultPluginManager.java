package cn.jowen.framework.plugin;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.plugin.event.PluginEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 默认插件管理器。维护注册表并按注册顺序编排生命周期，支持事件发布。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class DefaultPluginManager implements PluginManager {

    private final Map<String, Plugin> registry = new LinkedHashMap<>();
    private final PluginEventPublisher eventPublisher;

    public DefaultPluginManager() {
        this(null);
    }

    public DefaultPluginManager(@Nullable EventBus eventBus) {
        this.eventPublisher = eventBus != null ? new PluginEventPublisher(eventBus) : null;
    }

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
        if (eventPublisher != null) {
            eventPublisher.publishPluginLoaded(plugin);
            eventPublisher.publishPluginStarted(plugin);
        }
    }

    @Override
    public void load(PluginLoader loader) {
        PluginDescriptor descriptor = loader.descriptor();
        resolveDependencies(descriptor);
        Plugin plugin = loader.load();
        register(plugin);
    }

    private void resolveDependencies(PluginDescriptor descriptor) {
        List<String> deps = descriptor.dependencies();
        if (deps.isEmpty()) return;
        List<String> missing = new ArrayList<>();
        for (String dep : deps) {
            if (registry.get(dep) == null) missing.add(dep);
        }
        if (!missing.isEmpty()) {
            throw new PluginLoader.PluginException("插件 " + descriptor.id() + " 依赖未满足：" + missing);
        }
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
            if (eventPublisher != null) {
                eventPublisher.publishPluginStopped(plugin);
                eventPublisher.publishPluginUnloaded(plugin);
            }
        }
    }

    @Override
    public void stopAll() {
        List<Plugin> reversed = new ArrayList<>(registry.values());
        java.util.Collections.reverse(reversed);
        for (Plugin plugin : reversed) {
            try { plugin.destroy(); } catch (Exception ignored) {}
        }
        registry.clear();
    }

    @Override
    @Nullable
    public Plugin get(String id) {
        return registry.get(id);
    }

    @Override
    public List<Plugin> all() {
        return new ArrayList<>(registry.values());
    }

    @Nullable
    public PluginEventPublisher getEventPublisher() {
        return eventPublisher;
    }
}
