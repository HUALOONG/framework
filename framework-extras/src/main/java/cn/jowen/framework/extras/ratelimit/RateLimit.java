package cn.jowen.framework.extras.ratelimit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解。
 *
 * <p>用于标记方法或接口，限制请求频率。支持四种算法和多种作用域。
 *
 * <p>使用示例：
 * <pre>{@code
 * @RateLimit(key = "'api:user:list'", permits = 100, period = 1000, algorithm = SLIDING_WINDOW)
 * public List<User> listUsers() { ... }
 *
 * @RateLimit(key = "'api:order:create'", permits = 10, period = 60000, scope = USER)
 * public Order createOrder(OrderRequest request) { ... }
 * }</pre>
 *
 * @author 王飞
 * @since 2026-08-25
 * @see RateLimitInterceptor
 * @see RateLimitAlgorithm
 * @see RateLimitScope
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键（SpEL 表达式）。
     */
    String key();

    /**
     * 每秒允许的请求数。
     */
    int permits();

    /**
     * 统计周期（毫秒）。
     */
    long period();

    /**
     * 限流算法。
     */
    RateLimitAlgorithm algorithm() default RateLimitAlgorithm.SLIDING_WINDOW;

    /**
     * 限流作用域。
     */
    RateLimitScope scope() default RateLimitScope.GLOBAL;

    /**
     * 限流失败时的提示信息。
     */
    String message() default "请求过于频繁，请稍后重试";
}
