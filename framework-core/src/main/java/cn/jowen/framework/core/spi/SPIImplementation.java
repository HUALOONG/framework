package cn.jowen.framework.core.spi;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实现类为某 SPI 的具体实现，并指定其唯一名称与排序权重。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface SPIImplementation {

    /**
     * 实现名，用于 {@link ExtensionLoader#getExtension(String)} 按名获取。
     *
     * @return 实现名，不可为空
     */
    String name();

    /**
     * 排序权重，值越小优先级越高。仅对 {@link Activate} 自动激活集合有意义。
     *
     * @return 权重，默认 0
     */
    int order() default 0;
}
