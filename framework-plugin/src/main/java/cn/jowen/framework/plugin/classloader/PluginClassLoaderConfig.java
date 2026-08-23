package cn.jowen.framework.plugin.classloader;

import org.jspecify.annotations.NullMarked;

import java.util.Collections;
import java.util.List;

/**
 * 插件类加载全局配置持有器。零 Spring 依赖，仅以静态字段承载类加载策略与导出包，
 * 由框架装配层（{@code PluginAutoConfiguration}）在启动时写入，供 {@link PluginLoader} 读取。
 *
 * <p>设计上刻意与 Spring 解耦：本类不引用任何 Spring API，仅使用 {@link String} 与 {@link List}，
 * 以便 framework-plugin 在脱离 Spring 的场景下也能直接配置类加载策略。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class PluginClassLoaderConfig {

    private static volatile String strategy = "delegate";
    private static volatile List<String> exportedPackages = Collections.emptyList();

    private PluginClassLoaderConfig() {
        // 静态持有器，不可实例化
    }

    /**
     * 写入全局类加载策略与导出包。
     *
     * @param strategy          策略：{@code "delegate"} 或 {@code "isolated"}；{@code null} 视为默认 delegate
     * @param exportedPackages  导出包列表；{@code null} 视为空
     */
    public static void configure(String strategy, List<String> exportedPackages) {
        PluginClassLoaderConfig.strategy = strategy == null ? "delegate" : strategy;
        PluginClassLoaderConfig.exportedPackages =
                exportedPackages == null ? Collections.emptyList() : List.copyOf(exportedPackages);
    }

    /** @return 当前策略，默认 {@code "delegate"} */
    public static String strategy() {
        return strategy;
    }

    /** @return 当前导出包列表，不可为 {@code null} */
    public static List<String> exportedPackages() {
        return exportedPackages;
    }
}
