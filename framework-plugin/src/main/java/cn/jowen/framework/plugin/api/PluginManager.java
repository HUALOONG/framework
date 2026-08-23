package cn.jowen.framework.plugin.api;

import cn.jowen.framework.plugin.registry.Extension;
import cn.jowen.framework.plugin.registry.ExtensionPoint;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

/**
 * 插件管理器（核心门面）。负责插件的加载、卸载、生命周期编排与扩展点查询。
 *
 * @author 王飞
 */
@NullMarked
public interface PluginManager {

    /**
     * 从目录加载所有插件 jar。
     *
     * @param pluginsDir 插件目录，不可为 {@code null}
     * @return 已加载的插件列表，不可为 {@code null}
     */
    List<Plugin> loadPlugins(Path pluginsDir);

    /**
     * 加载单个插件 jar。
     *
     * @param pluginPath 插件 jar 路径，不可为 {@code null}
     * @return 加载的插件实例，不可为 {@code null}
     */
    Plugin loadPlugin(Path pluginPath);

    /**
     * 卸载指定插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    void unloadPlugin(String pluginId);

    /**
     * 启动指定插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    void startPlugin(String pluginId);

    /**
     * 停止指定插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    void stopPlugin(String pluginId);

    /**
     * 重启指定插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     */
    void restartPlugin(String pluginId);

    /**
     * 获取指定插件。
     *
     * @param pluginId 插件 id，不可为 {@code null}
     * @return 插件实例，不存在时返回 {@code null}
     */
    @Nullable
    Plugin getPlugin(String pluginId);

    /**
     * 获取所有已注册插件。
     *
     * @return 插件列表，不可为 {@code null}
     */
    List<Plugin> getPlugins();

    /**
     * 按状态筛选插件。
     *
     * @param state 目标状态，不可为 {@code null}
     * @return 匹配状态的插件列表，不可为 {@code null}
     */
    List<Plugin> getPluginsByState(PluginState state);

    /**
     * 按扩展点 ID 获取单个扩展。
     *
     * @param extensionPointId 扩展点 id，不可为 {@code null}
     * @return 扩展实例，不存在时返回 {@code null}
     */
    @Nullable
    Extension getExtension(String extensionPointId);

    /**
     * 按扩展点接口类型获取所有扩展实现。
     *
     * @param extensionPointClass 扩展点接口类型，不可为 {@code null}
     * @param <T>                 扩展点接口类型
     * @return 扩展实现列表，不可为 {@code null}（可能为空）
     */
    <T> List<T> getExtensions(Class<T> extensionPointClass);

    /**
     * 注册扩展点。
     *
     * @param point 扩展点，不可为 {@code null}
     */
    void registerExtensionPoint(ExtensionPoint point);
}
