package cn.jowen.framework.data.core.meta;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/**
 * 「Column」接口定义。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public @interface Column {
    String value() default "";
    boolean ignore() default false;
}
