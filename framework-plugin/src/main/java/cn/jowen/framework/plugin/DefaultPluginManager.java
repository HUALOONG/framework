package cn.jowen.framework.plugin;

import cn.jowen.framework.core.event.EventBus;
import cn.jowen.framework.plugin.dependency.DependencyResolutionException;
import cn.jowen.framework.plugin.dependency.DependencyResolver;
import cn.jowen.framework.plugin.event.PluginEventPublisher;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
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
    /** 已加载的 loader 引用，用于 unregister/stopAll 时关闭 classloader 释放 jar 文件句柄。 */
    private final Map<String, PluginLoader> loaders = new LinkedHashMap<>();
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
        // 检查已有注册表中的依赖（loadAll 场景由 DependencyResolver 批次内检查）
        List<String> missing = new ArrayList<>();
        for (String dep : descriptor.dependencies()) {
            if (registry.get(dep) == null) missing.add(dep);
        }
        if (!missing.isEmpty()) {
            throw new PluginLoader.PluginException("插件 " + descriptor.id() + " 依赖未满足：" + missing);
        }
        // 关闭旧 loader，释放旧 jar 文件句柄
        PluginLoader oldLoader = loaders.put(descriptor.id(), loader);
        if (oldLoader != null) {
            try { oldLoader.close(); } catch (IOException ignored) {}
        }
        Plugin plugin = loader.load();
        if (!plugin.id().equals(descriptor.id())) {
            throw new PluginLoader.PluginException("插件 id 与描述符 id 不一致：" + plugin.id() + " vs " + descriptor.id());
        }
        register(plugin);
    }

    @Override
    public List<Plugin> loadAll(List<PluginLoader> pluginLoaders) {
        List<PluginDescriptor> descriptors = new ArrayList<>(pluginLoaders.size());
        Map<String, PluginLoader> loaderById = new LinkedHashMap<>(pluginLoaders.size());
        for (PluginLoader loader : pluginLoaders) {
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
            // 关闭旧 loader，释放旧 jar 文件句柄
            PluginLoader oldLoader = loaders.put(id, loader);
            if (oldLoader != null) {
                try { oldLoader.close(); } catch (IOException ignored) {}
            }
            register(plugin);
            loaded.add(plugin);
        }
        return Collections.unmodifiableList(loaded);
    }

    @Override
    public void unregister(String id) {
        PluginLoader oldLoader = loaders.remove(id);
        if (oldLoader != null) {
            try { oldLoader.close(); } catch (IOException ignored) {}
        }
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
        for (PluginLoader loader : loaders.values()) {
            try { loader.close(); } catch (IOException ignored) {}
        }
        loaders.clear();
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
