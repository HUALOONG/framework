package cn.jowen.framework.plugin;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.plugin.dependency.DependencyResolutionException;
import cn.jowen.framework.plugin.dependency.DependencyResolver;
import cn.jowen.framework.plugin.event.PluginEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
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

    /** 注册表，{@link LinkedHashMap} 保证 {@link #all()} 与注册（即加载）顺序一致。 */
    private final Map<String, Plugin> registry = new LinkedHashMap<>();
    private final DependencyResolver dependencyResolver = new DependencyResolver();
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
            PluginDescriptor desc = plugin.descriptor();
            if (desc != null) {
                eventPublisher.publishPluginInstall(plugin, desc);
            }
            eventPublisher.publishPluginLoaded(plugin);
            eventPublisher.publishPluginStarted(plugin);
        }
    }

    @Override
    public void load(PluginLoader loader) {
        PluginDescriptor descriptor = loader.descriptor();
        resolveDependencies(descriptor);
        Plugin plugin = loader.load();
        if (!plugin.id().equals(descriptor.id())) {
            throw new PluginLoader.PluginException("插件 id 与描述符 id 不一致：" + plugin.id() + " vs " + descriptor.id());
        }
        register(plugin);
    }

    @Override
    public List<Plugin> loadAll(List<PluginLoader> loaders) {
        List<PluginDescriptor> descriptors = new ArrayList<>(loaders.size());
        Map<String, PluginLoader> loaderById = new LinkedHashMap<>(loaders.size());
        for (PluginLoader loader : loaders) {
            PluginDescriptor descriptor = loader.descriptor();
            descriptors.add(descriptor);
            if (loaderById.putIfAbsent(descriptor.id(), loader) != null) {
                throw new PluginLoader.PluginException("插件 id 重复：" + descriptor.id());
            }
        }

        // 依赖解析先行：循环依赖或批次内未满足依赖都在此抛出 DependencyResolutionException，
        // 此时尚未加载/注册任何插件，管理器状态保持不变。
        List<String> order = dependencyResolver.resolve(descriptors);

        List<Plugin> loaded = new ArrayList<>(order.size());
        for (String id : order) {
            PluginLoader loader = loaderById.get(id);
            if (loader == null) {
                throw new DependencyResolutionException("依赖解析结果包含未知插件 id：" + id);
            }
            Plugin plugin = loader.load();
            register(plugin);
            loaded.add(plugin);
        }
        return Collections.unmodifiableList(loaded);
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
