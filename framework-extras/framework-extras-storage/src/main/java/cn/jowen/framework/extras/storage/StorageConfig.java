package cn.jowen.framework.extras.storage;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 存储配置注解：声明存储桶名称与关联属性。
 *
 * <p>可标注于类或字段，用于声明该处使用的存储桶；未显式指定时使用默认桶。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface StorageConfig {

    /** 存储桶名称，空表示使用默认桶 */
    String bucket() default "";
}
