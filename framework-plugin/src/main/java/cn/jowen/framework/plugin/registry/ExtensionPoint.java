package cn.jowen.framework.plugin.registry;

import org.jspecify.annotations.NullMarked;

/**
 * 扩展点实体。
 *
 * @param id          扩展点唯一标识
 * @param type        扩展点接口类型全限定名
 * @param description 描述
 * @param singleton   是否单例
 * @author 王飞
 */
@NullMarked
public record ExtensionPoint(
        String id,
        String type,
        String description,
        boolean singleton
) {
    /**
     * 工厂方法。
     */
    public static ExtensionPoint of(String id, String type) {
        return new ExtensionPoint(id, type, "", false);
    }
}
