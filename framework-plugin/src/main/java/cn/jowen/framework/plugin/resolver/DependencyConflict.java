package cn.jowen.framework.plugin.resolver;

import org.jspecify.annotations.NullMarked;

/**
 * 依赖冲突信息。
 *
 * @param pluginId         插件 id
 * @param requiredBy       冲突来源插件 id
 * @param requiredVersions 要求的版本范围列表
 * @param resolved         是否已解决
 * @author 王飞
 */
@NullMarked
public record DependencyConflict(
        String pluginId,
        String requiredBy,
        java.util.List<String> requiredVersions,
        boolean resolved
) {
}
