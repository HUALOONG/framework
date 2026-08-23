package cn.jowen.framework.core.spi;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实现类需被自动激活（无需显式按名获取），常用于全局拦截器、过滤器等可插拔组件。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Activate {

    /**
     * 分组，用于按组筛选激活实现。为空表示不加分组限制。
     *
     * @return 分组名数组
     */
    String[] group() default {};

    /**
     * 排序权重，值越小优先级越高。
     *
     * @return 权重，默认 0
     */
    int order() default 0;
}
