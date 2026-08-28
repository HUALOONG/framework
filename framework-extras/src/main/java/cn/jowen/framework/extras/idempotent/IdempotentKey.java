package cn.jowen.framework.extras.idempotent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 幂等控制注解（KEY 模式）。
 *
 * <p>标注在需要幂等控制的方法上，由 {@link IdempotentKeyInterceptor} 拦截：
 * 按 SpEL 计算业务唯一键并执行 SETNX 占用，重复请求抛 {@link IdempotencyException}。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IdempotentKey {

    /**
     * 业务唯一键（支持 SpEL 表达式，如 {@code #orderId}、{@code #args[0]}）。
     */
    String key();

    /**
     * 重复请求时的异常消息。
     */
    String message() default "重复请求，请勿重复提交";
}