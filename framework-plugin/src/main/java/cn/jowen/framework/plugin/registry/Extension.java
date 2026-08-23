package cn.jowen.framework.plugin.registry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 扩展实现实体。
 *
 * @param id               扩展实现唯一标识
 * @param extensionPointId 所属扩展点 id，不可为 {@code null}
 * @param instance         扩展实现实例
 * @param order            排序权重（越小越优先）
 * @param pluginId         所属插件 id
 * @param properties       扩展属性
 * @author 王飞
 */
@NullMarked
public record Extension(
        String id,
        String extensionPointId,
        Object instance,
        int order,
        String pluginId,
        @Nullable Map<String, Object> properties
) {
}
