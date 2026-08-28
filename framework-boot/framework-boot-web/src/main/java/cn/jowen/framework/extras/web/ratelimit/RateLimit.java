package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级限流注解。
 *
 * <p>基于令牌桶算法，按 {@code key} 维度限制单位时间内的请求次数。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RateLimit {

    /** 限流维度 key，支持 SpEL，默认取方法全限定名 */
    String key() default "";

    /** 时间窗口内允许的最大请求数 */
    int permits() default 100;

    /** 时间窗口（秒） */
    int window() default 1;

    /** 超限后的提示信息 */
    String message() default "请求过于频繁，请稍后再试";
}
