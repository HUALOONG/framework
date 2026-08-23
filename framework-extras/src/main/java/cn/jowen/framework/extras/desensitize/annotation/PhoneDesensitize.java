package cn.jowen.framework.extras.desensitize.annotation;

import cn.jowen.framework.extras.desensitize.serializer.DesensitizeJsonSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 手机号脱敏注解（便捷封装）。
 *
 * <p>等价于元标注 {@link DesensitizeMeta#strategy() = "PHONE"}，序列化时由
 * {@link DesensitizeJsonSerializer} 读取并调用核心 {@code Desensitizer} 脱敏，输出如 {@code 138****5678}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@DesensitizeMeta(strategy = "PHONE")
@JsonSerialize(using = DesensitizeJsonSerializer.class)
public @interface PhoneDesensitize {

    /**
     * 是否跳过脱敏（管理员免脱敏）。
     */
    boolean skip() default false;
}
