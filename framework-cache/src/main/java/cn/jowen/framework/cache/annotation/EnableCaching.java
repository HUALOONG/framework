package cn.jowen.framework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 启用缓存注解支持。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableCaching {
    /**
     * 是否使用 CGLIB 代理
     */
    boolean proxyTargetClass() default false;
}
