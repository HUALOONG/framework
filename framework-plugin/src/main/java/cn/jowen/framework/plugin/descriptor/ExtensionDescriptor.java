package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 扩展实现描述符。
 *
 * @param id               扩展实现唯一标识
 * @param extensionPointId 所属扩展点 id，不可为 {@code null}
 * @param className        实现类全限定名，不可为 {@code null}
 * @param order            排序权重（越小越优先）
 * @param properties       扩展属性
 * @author 王飞
 */
@NullMarked
public record ExtensionDescriptor(
        String id,
        String extensionPointId,
        String className,
        int order,
        @Nullable Map<String, Object> properties
) {
    /**
     * 从 map 构建。
     *
     * @param map 扩展实现描述符 map
     * @return 扩展实现描述符
     */
    @SuppressWarnings("unchecked")
    public static ExtensionDescriptor fromMap(Map<String, Object> map) {
        String id = (String) map.get("id");
        String extensionPointId = (String) map.get("extensionPointId");
        String className = (String) map.get("className");
        int order = map.get("order") != null ? ((Number) map.get("order")).intValue() : 0;
        Map<String, Object> properties = (Map<String, Object>) map.get("properties");
        return new ExtensionDescriptor(id, extensionPointId, className, order, properties);
    }
}
