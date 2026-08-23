package cn.jowen.framework.plugin;

import org.jspecify.annotations.NullMarked;

/**
 * 插件可选择的 Spring 子容器感知接口。
 *
 * <p>实现此接口的插件会在 {@link cn.jowen.framework.plugin.config.PluginApplicationContext#registerPlugin(Plugin)}
 * 时被注入子容器引用，可通过 {@link #setSpringContext} 访问宿主 Bean。
 * 卸载时若容器已配置为此插件创建，则自动关闭子容器。
 *
 * @author Jowen
 */
@NullMarked
public interface SpringContextAware {
    /**
     * 注入 Spring 子容器引用。
     *
     * @param context 子容器，不可为 {@code null}
     */
    void setSpringContext(Object context);

    /**
     * 清理 Spring 子容器引用（可选，用于显式释放）。
     */
    default void closeSpringContext() {}
}
