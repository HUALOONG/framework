package cn.jowen.framework.plugin.lifecycle;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.Plugin;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.event.PluginEvent;
import cn.jowen.framework.plugin.event.PluginEventListener;
import cn.jowen.framework.plugin.event.PluginFailedEvent;
import cn.jowen.framework.plugin.event.PluginStartedEvent;
import cn.jowen.framework.plugin.event.PluginStartingEvent;
import cn.jowen.framework.plugin.event.PluginStoppedEvent;
import cn.jowen.framework.plugin.event.PluginStoppingEvent;
import cn.jowen.framework.plugin.event.PluginUnloadedEvent;
import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionPoint;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 插件生命周期管理器。状态机驱动插件的 initialize/start/stop/restart/destroy。
 *
 * @author 王飞
 */
@NullMarked
public final class PluginLifecycleManager implements PluginManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginLifecycleManager.class);

    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final Map<String, AtomicReference<PluginState>> states = new ConcurrentHashMap<>();
    private final Map<String, PluginContext> contexts = new ConcurrentHashMap<>();
    private final List<PluginHealthChecker> healthCheckers = new ArrayList<>();
    private final List<PluginEventListener> listeners = new ArrayList<>();
    private @Nullable ExtensionRegistry extensionRegistry;

    /**
     * 全局生命周期锁：保证对同一插件的 initialize/start/restart/destroy 串行执行，
     * 避免并发下 destroy 移除注册表后 start 仍使用旧上下文导致的 NPE 或重复启动竞态。
     * 注意：{@link #stop(String)} 作为 destroy/restart 的内部调用方不再加锁，以免重入死锁。
     */
    private final ReentrantLock lifecycleLock = new ReentrantLock();

    /**
     * 初始化插件（注入上下文，状态 CREATED → STARTING）。
     */
    @Override
    public void initialize(String pluginId, Plugin plugin, PluginContext context) {
        lifecycleLock.lock();
        try {
            plugins.put(pluginId, plugin);
            states.put(pluginId, new AtomicReference<>(PluginState.CREATED));
            contexts.put(pluginId, context);
            transition(pluginId, PluginState.STARTING);
            publish(new PluginStartingEvent(pluginId));
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 启动插件（校验依赖 → 注册扩展点 → plugin.start() → STARTED）。
     */
    public void start(String pluginId) {
        lifecycleLock.lock();
        try {
            Plugin plugin = plugins.get(pluginId);
            if (plugin == null) return;
            try {
                PluginContext ctx = contexts.get(pluginId);
                if (ctx == null) {
                    throw new IllegalStateException("插件 " + pluginId + " 未初始化");
                }
                if (getState(pluginId) == PluginState.STOPPED) {
                    transition(pluginId, PluginState.STARTING);
                }
                plugin.start(ctx);
                transition(pluginId, PluginState.STARTED);
                publish(new PluginStartedEvent(pluginId));
            } catch (Exception e) {
                transition(pluginId, PluginState.FAILED);
                publish(new PluginFailedEvent(pluginId, e.getMessage()));
                throw new RuntimeException("插件启动失败：" + pluginId, e);
            }
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 停止插件（plugin.stop() → STOPPED）。
     */
    public void stop(String pluginId) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin == null) return;
        try {
            if (getState(pluginId) != PluginState.STARTING) {
                transition(pluginId, PluginState.STOPPING);
                publish(new PluginStoppingEvent(pluginId));
            }
            plugin.stop();
            transition(pluginId, PluginState.STOPPED);
            publish(new PluginStoppedEvent(pluginId));
        } catch (Exception e) {
            transition(pluginId, PluginState.FAILED);
            publish(new PluginFailedEvent(pluginId, e.getMessage()));
            throw new RuntimeException("插件停止失败：" + pluginId, e);
        }
    }

    /**
     * 重启插件（stop + start）。
     */
    public void restart(String pluginId) {
        lifecycleLock.lock();
        try {
            stop(pluginId);
            start(pluginId);
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 销毁插件（stop + 清理注册 + 关闭 ClassLoader + 移除注册）。
     */
    public void destroy(String pluginId) {
        lifecycleLock.lock();
        try {
            stop(pluginId);
            // 清理该插件在扩展注册中心的扩展，避免卸载后残留可被查询，并触发桥接源缓存失效
            if (extensionRegistry != null) {
                extensionRegistry.unregisterPlugin(pluginId);
            }
            plugins.remove(pluginId);
            states.remove(pluginId);
            contexts.remove(pluginId);
            publish(new PluginUnloadedEvent(pluginId));
        } finally {
            lifecycleLock.unlock();
        }
    }

    /**
     * 获取插件当前状态。
     */
    public PluginState getState(String pluginId) {
        AtomicReference<PluginState> ref = states.get(pluginId);
        return ref != null ? ref.get() : PluginState.CREATED;
    }

    /**
     * 获取插件上下文。
     */
    public @Nullable PluginContext getContext(String pluginId) {
        return contexts.get(pluginId);
    }

    // region PluginManager 接口实现

    @Override
    public List<Plugin> loadPlugins(Path pluginsDir) {
        throw new UnsupportedOperationException("请使用 PluginLoader 直接加载后调用 initialize");
    }

    @Override
    public Plugin loadPlugin(Path pluginPath) {
        throw new UnsupportedOperationException("请使用 PluginLoader 直接加载后调用 initialize");
    }

    @Override
    public void unloadPlugin(String pluginId) {
        destroy(pluginId);
    }

    @Override
    public void startPlugin(String pluginId) {
        start(pluginId);
    }

    @Override
    public void stopPlugin(String pluginId) {
        stop(pluginId);
    }

    @Override
    public void restartPlugin(String pluginId) {
        restart(pluginId);
    }

    @Override
    public @Nullable Plugin getPlugin(String pluginId) {
        return plugins.get(pluginId);
    }

    @Override
    public List<Plugin> getPlugins() {
        return List.copyOf(plugins.values());
    }

    @Override
    public List<Plugin> getPluginsByState(PluginState state) {
        List<Plugin> result = new ArrayList<>();
        for (Map.Entry<String, AtomicReference<PluginState>> e : states.entrySet()) {
            if (e.getValue().get() == state) {
                Plugin p = plugins.get(e.getKey());
                if (p != null) result.add(p);
            }
        }
        return result;
    }

    @Override
    public @Nullable Extension getExtension(String extensionPointId) {
        if (extensionRegistry == null) {
            return null;
        }
        List<Extension> exts = extensionRegistry.getExtensions(extensionPointId);
        return exts.isEmpty() ? null : exts.getFirst();
    }

    @Override
    public <T> List<T> getExtensions(Class<T> extensionPointClass) {
        if (extensionRegistry == null) {
            return List.of();
        }
        return extensionRegistry.getExtensionsByType(extensionPointClass);
    }

    @Override
    public void registerExtensionPoint(ExtensionPoint point) {
        // 扩展点描述符由描述符加载链注册；本管理器仅委托扩展实例查询至 ExtensionRegistry
    }

    // endregion
    private void transition(String pluginId, PluginState target) {
        AtomicReference<PluginState> ref = states.get(pluginId);
        if (ref == null) return;
        PluginStateTransition.transition(ref, target);
    }

    private void publish(PluginEvent event) {
        for (PluginEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                LOGGER.warn("事件监听器异常：" + e.getMessage());
            }
        }
    }

    public void addHealthChecker(PluginHealthChecker checker) {
        healthCheckers.add(checker);
    }

    public void addListener(PluginEventListener listener) {
        listeners.add(listener);
    }

    /**
     * 注入扩展注册中心，使 {@code getExtension(s)} 委托其查询；未注入时返回空。
     *
     * @param extensionRegistry 扩展注册中心，可为 {@code null}
     */
    public void setExtensionRegistry(@Nullable ExtensionRegistry extensionRegistry) {
        this.extensionRegistry = extensionRegistry;
    }

    public List<PluginHealthChecker> getHealthCheckers() {
        return List.copyOf(healthCheckers);
    }
}
