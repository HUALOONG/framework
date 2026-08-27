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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

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

    /**
     * 初始化插件（注入上下文，状态 CREATED → STARTING）。
     */
    public void initialize(String pluginId, Plugin plugin, PluginContext context) {
        plugins.put(pluginId, plugin);
        states.put(pluginId, new AtomicReference<>(PluginState.CREATED));
        contexts.put(pluginId, context);
        transition(pluginId, PluginState.STARTING);
        publish(new PluginStartingEvent(pluginId));
    }

    /**
     * 启动插件（校验依赖 → 注册扩展点 → plugin.start() → STARTED）。
     */
    public void start(String pluginId) {
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
        stop(pluginId);
        start(pluginId);
    }

    /**
     * 销毁插件（stop + 关闭 ClassLoader + 移除注册）。
     */
    public void destroy(String pluginId) {
        stop(pluginId);
        plugins.remove(pluginId);
        states.remove(pluginId);
        contexts.remove(pluginId);
        publish(new PluginUnloadedEvent(pluginId));
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
    public Extension getExtension(String extensionPointId) {
        return null; // 由 ExtensionRegistry 处理
    }

    @Override
    public <T> List<T> getExtensions(Class<T> extensionPointClass) {
        return List.of(); // 由 ExtensionRegistry 处理
    }

    @Override
    public void registerExtensionPoint(ExtensionPoint point) {
        // 由 ExtensionRegistry 处理
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

    public List<PluginHealthChecker> getHealthCheckers() {
        return List.copyOf(healthCheckers);
    }
}
