package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * 插件依赖声明。
 *
 * @param pluginId     依赖的插件 id，不可为 {@code null}
 * @param versionRange 版本范围，不可为 {@code null}
 * @param optional     是否为可选依赖
 * @author 王飞
 */
@NullMarked
public record PluginDependency(String pluginId, VersionRange versionRange, boolean optional) {

    /**
     * 从 map 构建依赖。
     *
     * @param map 描述符 map，不可为 {@code null}
     * @return 依赖对象
     */
    @SuppressWarnings("unchecked")
    public static PluginDependency fromMap(Map<String, Object> map) {
        String pluginId = (String) map.get("pluginId");
        String versionRangeStr = (String) map.get("versionRange");
        boolean optional = Boolean.TRUE.equals(map.get("optional"));
        VersionRange range = versionRangeStr != null ? new VersionRange(versionRangeStr) : null;
        return new PluginDependency(pluginId, range, optional);
    }
}
