package cn.jowen.framework.plugin;

import cn.jowen.framework.plugin.dependency.DependencyResolutionException;
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
     * 批量加载一组插件加载器，并按依赖拓扑序（依赖方在前）注册启动。
     *
     * <p>依赖解析在加载任何插件<b>之前</b>一次性完成：若批次内存在循环依赖，或某插件声明的依赖
     * 不在本批次中，则整批拒绝——不会加载、注册或启动其中任何一个插件，管理器状态保持不变。
     *
     * <p>注意：依赖必须在<b>本批次内自包含</b>。即使某依赖已经通过 {@link #register(Plugin)} 或
     * {@link #load(PluginLoader)} 注册在管理器中，只要它不在 {@code loaders} 里，仍会被判定为
     * 未满足依赖。需要跨批次依赖时，请把相关插件放在同一批次加载。
     *
     * @param loaders 插件加载器列表，不可为 {@code null}；空列表返回空结果
     * @return 已加载并启动的插件，顺序与拓扑序一致，不可修改
     * @throws DependencyResolutionException 存在循环依赖或批次内未满足依赖时原样透传
     * @throws PluginException 单个插件实例化或启动失败时抛出
     */
    List<Plugin> loadAll(List<PluginLoader> loaders);

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
