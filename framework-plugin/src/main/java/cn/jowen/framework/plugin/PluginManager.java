package cn.jowen.framework.plugin;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 插件管理器。负责插件的注册、生命周期编排与发现。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public interface PluginManager {

    /**
     * 注册并启动一个插件实例。
     *
     * @param plugin 插件实例，不可为 {@code null}
     * @throws PluginException 重复 id 或启动失败时抛出
     */
    void register(Plugin plugin);

    /**
     * 从类加载器加载插件并注册启动。
     *
     * @param loader 插件加载器，不可为 {@code null}
     */
    void load(PluginLoader loader);

    /**
     * 停止并注销插件。
     *
     * @param id 插件 id，不可为 {@code null}
     */
    void unregister(String id);

    /** 停止全部插件（逆注册顺序）。 */
    void stopAll();

    /**
     * 获取已注册插件。
     *
     * @param id 插件 id，不可为 {@code null}
     * @return 插件实例或 {@code null}
     */
    @Nullable Plugin get(String id);

    /** @return 全部已注册插件（按注册顺序） */
    List<Plugin> all();
}
