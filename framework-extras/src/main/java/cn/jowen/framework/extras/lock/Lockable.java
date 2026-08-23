package cn.jowen.framework.extras.lock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式分布式锁注解。
 *
 * <p>标注在需要加锁的方法上，由 {@link LockInterceptor} 拦截并执行锁逻辑。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Lockable {

    /**
     * 锁 key（支持 SpEL 表达式）。
     */
    String key();

    /**
     * 最大等待时间（毫秒），缺省 5000。
     */
    long waitTime() default 5000;

    /**
     * 锁持有时间（毫秒），缺省 30000。
     */
    long leaseTime() default 30000;

    /**
     * 锁类型，缺省可重入锁。
     */
    LockType lockType() default LockType.REENTRANT;

    /**
     * 获取锁失败消息，缺省抛 LockException。
     */
    String failMessage() default "";

    /**
     * 降级处理 Bean 名（Spring 环境），为空则直接抛异常。
     */
    String fallback() default "";
}
