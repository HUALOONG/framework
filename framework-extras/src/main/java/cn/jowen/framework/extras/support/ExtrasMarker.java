package cn.jowen.framework.extras.support;

import org.jspecify.annotations.NullMarked;

/**
 * Extras 装配标记接口。
 *
 * <p>供 {@code framework-boot-autoconfigure} 通过 {@code @ConditionalOnClass} 判断
 * extras 模块是否可用，避免不必要的 Bean 注册。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public interface ExtrasMarker {
}
