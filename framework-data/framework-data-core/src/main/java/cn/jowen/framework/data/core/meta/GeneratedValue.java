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
public @interface GeneratedValue {
    Strategy value() default Strategy.AUTO;
    enum Strategy { AUTO, SNOWFLAKE, UUID, ASSIGNED }
}
