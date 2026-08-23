package cn.jowen.framework.plugin.api;

import cn.jowen.framework.plugin.context.PluginConfiguration;
import cn.jowen.framework.plugin.context.SharedData;
import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import cn.jowen.framework.plugin.event.PluginEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;

/**
 * 插件运行时上下文。提供插件访问框架共享数据、发布事件、获取 ClassLoader 等能力。
 *
 * @author 王飞
 */
@NullMarked
public interface PluginContext extends Closeable {

    /**
     * 返回本上下文所属的插件 id。
     *
     * @return 插件 id，不可为 {@code null}
     */
    String getPluginId();

    /**
     * 返回本插件的静态描述符。
     *
     * @return 插件描述符，不可为 {@code null}
     */
    PluginDescriptor getPluginDescriptor();

    /**
     * 返回插件配置读取器。
     *
     * @return 配置对象，不可为 {@code null}
     */
    PluginConfiguration getConfiguration();

    /**
     * 返回插件间共享数据。
     *
     * @return 共享数据，不可为 {@code null}
     */
    SharedData getSharedData();

    /**
     * 返回插件管理器。
     *
     * @return 插件管理器，不可为 {@code null}
     */
    PluginManager getPluginManager();

    /**
     * 返回宿主（应用）类加载器。
     *
     * @return 宿主类加载器，不可为 {@code null}
     */
    ClassLoader getApplicationClassLoader();

    /**
     * 返回本插件的类加载器。
     *
     * @return 插件类加载器，不可为 {@code null}
     */
    ClassLoader getPluginClassLoader();

    /**
     * 返回本插件关联的 Spring 子容器（若已启用）。
     *
     * @return Spring 子容器，未启用时返回 {@code null}
     */
    @Nullable
    Object getSpringContext();

    /**
     * 发布一条插件事件。
     *
     * @param event 事件对象，不可为 {@code null}
     */
    void publishEvent(PluginEvent event);

    /**
     * 关闭上下文资源。
     */
    @Override
    void close();
}
