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
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public record PluginDescriptor(String id, String version, String className,
                               String description, List<String> dependencies) {

    public PluginDescriptor {
        dependencies = dependencies == null ? List.of() : List.copyOf(dependencies);
    }

    /** @return 无依赖的轻量描述符 */
    public static PluginDescriptor of(String id, String version, String className) {
        return new PluginDescriptor(id, version, className, "", List.of());
    }
}
