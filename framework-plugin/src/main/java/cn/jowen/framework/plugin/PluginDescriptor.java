package cn.jowen.framework.plugin;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 插件静态描述符。承载插件元数据，用于加载前的校验与依赖解析。
 *
 * @param id          唯一标识
 * @param version     版本
 * @param className   实现类全限定名
 * @param description 描述
 * @param dependencies 依赖的其他插件 id 列表
 * @param exportedPackages 导出给框架访问的包列表
 * @param springEnabled 是否启用 Spring 组件扫描
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public record PluginDescriptor(String id, String version, String className,
                               String description, List<String> dependencies,
                               List<String> exportedPackages, boolean springEnabled) {

    public PluginDescriptor(String id, String version, String className,
                            String description, List<String> dependencies) {
        this(id, version, className, description, dependencies, List.of(), false);
    }

    public PluginDescriptor {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
        exportedPackages = exportedPackages == null ? List.of() : List.copyOf(exportedPackages);
    }

    /** @return 无依赖的轻量描述符 */
    public static PluginDescriptor of(String id, String version, String className) {
        return new PluginDescriptor(id, version, className, "", List.of());
    }

    /** @return 无依赖的轻量描述符（含导出包） */
    public static PluginDescriptor of(String id, String version, String className, List<String> exportedPackages) {
        return new PluginDescriptor(id, version, className, "", List.of(), exportedPackages, false);
    }
}
