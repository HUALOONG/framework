package cn.jowen.framework.extras.web.idempotent;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 方法级幂等防重注解。
 *
 * <p>以 {@code key} 为维度记录请求指纹，在 {@code expire} 时间内重复请求将被拒绝。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Idempotent {

    /** 幂等 key，支持 SpEL */
    String key();

    /** 指纹保留时间（秒） */
    int expire() default 300;

    /** 重复提交提示 */
    String message() default "请勿重复提交";
}
