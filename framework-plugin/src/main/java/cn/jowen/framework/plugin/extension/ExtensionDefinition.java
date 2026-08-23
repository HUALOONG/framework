package cn.jowen.framework.plugin.extension;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 扩展定义。用于从 plugin.json 配置加载扩展时描述扩展实现。
 *
 * @param id               扩展实现唯一标识
 * @param extensionPointId 所属扩展点 id
 * @param className        实现类全限定名
 * @param order            排序权重
 * @param properties       扩展属性
 * @author 王飞
 */
@NullMarked
public record ExtensionDefinition(
        String id,
        String extensionPointId,
        String className,
        int order,
        @Nullable Map<String, Object> properties
) {
    /**
     * 使用指定 ClassLoader 实例化扩展实现。
     *
     * @param classLoader 类加载器，不可为 {@code null}
     * @return 实例，失败时返回 {@code null}
     */
    public @Nullable Object instantiate(ClassLoader classLoader) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
