package cn.jowen.framework.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 缓存读取注解。命中则直接返回，未命中执行方法并缓存结果。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    /**
     * 缓存名称，不可为空
     */
    String value();

    /**
     * 缓存键（支持 SpEL 表达式）
     */
    String key() default "";

    /**
     * 条件表达式（SpEL），满足条件才缓存
     */
    String condition() default "";

    /**
     * 否定条件表达式（SpEL），满足条件则不缓存
     */
    String unless() default "";

    /**
     * 是否同步加载（默认 false）
     */
    boolean sync() default false;

    /**
     * 缓存键生成器 Bean 名（可选，未指定则使用默认生成器）
     */
    String keyGenerator() default "";

    /**
     * 事件监听器 Bean 名
     */
    String listener() default "";
}
