package cn.jowen.framework.data.core.meta;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体字段对应的列名。缺省时按字段名驼峰转下划线推断。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    /**
     * 列名。为空表示按字段名推断。
     *
     * @return 列名
     */
    String value() default "";

    /**
     * 是否忽略该字段（不持久化）。
     *
     * @return 忽略返回 {@code true}
     */
    boolean ignore() default false;
}
