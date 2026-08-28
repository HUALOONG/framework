package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级分布式/本地锁注解。标注于方法上，执行期间对 {@code key} 维度加锁。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Lockable {

    /** 锁键，支持 SpEL，默认取类名+方法名 */
    String key() default "";

    /** 锁类型 */
    LockType type() default LockType.LOCAL;

    /** 获取锁最大等待时间（毫秒），<=0 表示不等待 */
    long waitMillis() default 0;

    /** 锁租约时间（毫秒），仅分布式锁生效，<=0 表示使用实现默认值 */
    long leaseMillis() default 0;

    /** 获取锁失败提示 */
    String message() default "当前资源正被占用，请稍后再试";
}
