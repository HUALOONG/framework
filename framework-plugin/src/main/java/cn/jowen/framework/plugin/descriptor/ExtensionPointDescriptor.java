package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;

/**
 * 扩展点描述符。
 *
 * @param id            扩展点唯一标识
 * @param interfaceName 扩展点接口全限定名
 * @param singleton     是否单例
 * @author 王飞
 */
@NullMarked
public record ExtensionPointDescriptor(
        String id,
        String interfaceName,
        boolean singleton
) {
}
