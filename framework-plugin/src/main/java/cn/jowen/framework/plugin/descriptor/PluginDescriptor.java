package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 插件静态描述符。承载插件完整元数据，用于加载前的校验、依赖解析与生命周期管理。
 *
 * @param pluginId         唯一标识
 * @param pluginName       插件显示名称
 * @param version          版本（语义化）
 * @param description      描述
 * @param author           作者
 * @param license          许可证
 * @param pluginClass      插件主类全限定名
 * @param requires         必需依赖插件列表
 * @param optionalRequires 可选依赖插件列表
 * @param provides         本插件提供的能力标识列表
 * @param extensionPoints  扩展点描述列表
 * @param extensions       扩展实现描述列表
 * @param configuration    配置项描述
 * @param enabledByDefault 是否默认启用
 * @author 王飞
 */
@NullMarked
public record PluginDescriptor(
        String pluginId,
        String pluginName,
        String version,
        String description,
        String author,
        String license,
        String pluginClass,
        List<PluginDependency> requires,
        List<PluginDependency> optionalRequires,
        List<String> provides,
        List<ExtensionPointDescriptor> extensionPoints,
        List<ExtensionDescriptor> extensions,
        @Nullable PluginConfigurationDescriptor configuration,
        boolean enabledByDefault
) {
    /**
     * 简化的单参构造器（仅 pluginId）。
     */
    public PluginDescriptor(String pluginId) {
        this(pluginId, pluginId, "0.0.0", "", "", "", pluginId,
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
    }

    /**
     * 无参工厂方法：创建最小可用描述符。
     */
    public static PluginDescriptor of(String pluginId, String version, String pluginClass) {
        return new PluginDescriptor(pluginId, pluginId, version, "", "", "", pluginClass,
                List.of(), List.of(), List.of(), List.of(), List.of(), null, true);
    }

    /**
     * 校验描述符完整性。
     *
     * @return 校验错误列表，为空表示合法
     */
    public List<cn.jowen.framework.plugin.support.ValidationError> validate() {
        List<cn.jowen.framework.plugin.support.ValidationError> errors = new java.util.ArrayList<>();
        if (pluginId == null || pluginId.isBlank()) {
            errors.add(new cn.jowen.framework.plugin.support.ValidationError("pluginId", "pluginId 不能为空"));
        }
        if (version == null || version.isBlank()) {
            errors.add(new cn.jowen.framework.plugin.support.ValidationError("version", "version 不能为空"));
        }
        if (pluginClass == null || pluginClass.isBlank()) {
            errors.add(new cn.jowen.framework.plugin.support.ValidationError("pluginClass", "pluginClass 不能为空"));
        }
        return errors;
    }
}
