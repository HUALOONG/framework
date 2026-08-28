package cn.jowen.framework.plugin.context;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.api.PluginContext;
import cn.jowen.framework.plugin.api.PluginManager;
import cn.jowen.framework.plugin.api.PluginState;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.event.PluginEvent;
import cn.jowen.framework.plugin.event.PluginEventListener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认插件上下文实现。
 *
 * @author 王飞
 */
@NullMarked
public final class DefaultPluginContext implements PluginContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPluginContext.class);

    /**
     * 插件 id
     */
    private final String pluginId;

    /**
     * 插件描述符
     */
    private final PluginDescriptor descriptor;

    /**
     * 插件配置
     */
    private final PluginConfiguration configuration;

    /**
     * 共享数据
     */
    private final SharedData sharedData;

    /**
     * 插件管理器
     */
    private final @Nullable PluginManager pluginManager;

    /**
     * 宿主类加载器
     */
    private final ClassLoader applicationClassLoader;

    /**
     * 插件类加载器
     */
    private final ClassLoader pluginClassLoader;

    /**
     * Spring 上下文
     */
    private final @Nullable Object springContext;

    /**
     * 事件监听器列表
     */
    private final List<PluginEventListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * 插件状态
     */
    private volatile PluginState state = PluginState.CREATED;

    /**
     * 构造函数。
     *
     * @param pluginId               插件 id
     * @param descriptor             插件描述符
     * @param configuration          插件配置
     * @param sharedData             共享数据
     * @param pluginManager          插件管理器
     * @param applicationClassLoader 宿主类加载器
     * @param pluginClassLoader      插件类加载器
     * @param springContext          Spring 上下文
     */
    public DefaultPluginContext(String pluginId, PluginDescriptor descriptor,
                                PluginConfiguration configuration,
                                SharedData sharedData,
                                @Nullable PluginManager pluginManager,
                                ClassLoader applicationClassLoader,
                                ClassLoader pluginClassLoader,
                                @Nullable Object springContext) {
        this.pluginId = pluginId;
        this.descriptor = descriptor;
        this.configuration = configuration;
        this.sharedData = sharedData;
        this.pluginManager = pluginManager;
        this.applicationClassLoader = applicationClassLoader;
        this.pluginClassLoader = pluginClassLoader;
        this.springContext = springContext;
    }

    /**
     * 获取插件id
     *
     * @return 插件id
     */
    @Override
    public String getPluginId() {
        return pluginId;
    }

    /**
     * 获取插件描述符
     *
     * @return 插件描述符
     */
    @Override
    public PluginDescriptor getPluginDescriptor() {
        return descriptor;
    }

    /**
     * 获取插件配置
     *
     * @return 插件配置
     */
    @Override
    public PluginConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * 获取共享数据
     *
     * @return 共享数据
     */
    @Override
    public SharedData getSharedData() {
        return sharedData;
    }

    /**
     * 获取插件管理器
     *
     * @return 插件管理器
     */
    @Override
    public PluginManager getPluginManager() {
        return pluginManager;
    }

    /**
     * 获取宿主类加载器
     *
     * @return 宿主类加载器
     */
    @Override
    public ClassLoader getApplicationClassLoader() {
        return applicationClassLoader;
    }

    /**
     * 获取插件类加载器
     *
     * @return 插件类加载器
     */
    @Override
    public ClassLoader getPluginClassLoader() {
        return pluginClassLoader;
    }

    /**
     * 获取Spring上下文
     *
     * @return Spring上下文
     */
    @Override
    public @Nullable Object getSpringContext() {
        return springContext;
    }

    /**
     * 发布插件事件
     *
     * @param event 事件对象，不可为 {@code null}
     */
    @Override
    public void publishEvent(PluginEvent event) {
        if (event == null) return;
        for (PluginEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                LOGGER.warn("事件监听器异常：" + e.getMessage());
            }
        }
    }

    /**
     * 添加事件监听器
     *
     * @param listener 事件监听器
     */
    public void addListener(PluginEventListener listener) {
        listeners.add(listener);
    }

    /**
     * 获取插件状态
     *
     * @return 插件状态
     */
    public PluginState getState() {
        return state;
    }

    public void setState(PluginState state) {
        if (!this.state.canTransitTo(state)) {
            throw new IllegalStateException(
                    "非法状态转换: " + this.state + " -> " + state + " [plugin=" + pluginId + "]");
        }
        this.state = state;
    }

    /**
     * 关闭上下文资源
     */
    @Override
    public void close() {
        listeners.clear();
        if (springContext instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                LOGGER.warn("插件 Spring 上下文关闭异常：" + e.getMessage());
            }
        }
        // 尽力置为停止态；非法转换（如未启动即关闭）仅记录，不抛出
        try {
            setState(PluginState.STOPPED);
        } catch (IllegalStateException e) {
            LOGGER.warn("插件上下文关闭状态置位跳过：" + e.getMessage());
        }
    }
}
