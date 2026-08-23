package cn.jowen.framework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存清除注解。方法执行后清除指定缓存键或全部缓存。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {
    /**
     * 缓存名称，不可为空
     */
    String value();

    /**
     * 缓存键（支持 SpEL 表达式）
     */
    String key() default "";

    /**
     * 是否清除所有缓存
     */
    boolean allEntries() default false;

    /**
     * 是否在方法执行前清除
     */
    boolean beforeInvocation() default false;

    /**
     * 缓存键生成器 Bean 名（可选，未指定则使用默认生成器）
     */
    String keyGenerator() default "";

    /**
     * 事件监听器 Bean 名
     */
    String listener() default "";
}
