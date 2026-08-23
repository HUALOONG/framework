package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 插件配置项描述符。
 *
 * @param key          配置键
 * @param type         配置类型（string/boolean/int/list/map）
 * @param defaultValue 默认值
 * @param required     是否必填
 * @param description  描述
 * @author 王飞
 */
@NullMarked
public record PluginConfigurationDescriptor(
        String key,
        String type,
        @Nullable String defaultValue,
        boolean required,
        String description
) {
}
