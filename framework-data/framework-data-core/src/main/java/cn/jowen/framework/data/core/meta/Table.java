package cn.jowen.framework.data.core.meta;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记实体类对应的数据库表名。缺省时由实现层按类名驼峰转下划线推断。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Table {

    /**
     * 表名。为空表示按类名推断。
     *
     * @return 表名
     */
    String value() default "";
}
