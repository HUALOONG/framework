package cn.jowen.framework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存写入注解。方法执行后更新缓存（不跳过方法执行）。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CachePut {
    /**
     * 缓存名称，不可为空
     */
    String value();

    /**
     * 缓存键（支持 SpEL 表达式）
     */
    String key() default "";

    /**
     * 条件表达式（SpEL）
     */
    String condition() default "";

    /**
     * 否定条件表达式（SpEL）
     */
    String unless() default "";

    /**
     * 缓存键生成器 Bean 名（可选，未指定则使用默认生成器）
     */
    String keyGenerator() default "";

    /**
     * 事件监听器 Bean 名
     */
    String listener() default "";
}
