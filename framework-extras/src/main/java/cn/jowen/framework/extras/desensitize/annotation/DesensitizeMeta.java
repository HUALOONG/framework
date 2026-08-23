package cn.jowen.framework.extras.desensitize.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 脱敏元注解（extras 自有）。
 *
 * <p>core 的 {@code @DesensitizeField} 仅声明 {@code @Target(ElementType.FIELD)}，
 * 无法作为元注解挂到便捷注解上，故在本模块定义具备
 * {@code @Target({FIELD, ANNOTATION_TYPE})} 的等价元注解，用于标注/元标注脱敏字段。
 *
 * <p>便捷注解（如 {@link PhoneDesensitize}）元标注本注解并设置固定 {@code strategy()}；
 * 同名字段（{@code skip}/{@code startKeep}/{@code endKeep}/{@code replacement}）在
 * 使用处的值由 {@code DesensitizeJsonSerializer} 通过反射从便捷注解实例读取。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
public @interface DesensitizeMeta {

    /**
     * 脱敏策略名（对应 {@code DesensitizeStrategies}）。
     */
    String strategy() default "PHONE";

    /**
     * 开头保留位数（覆盖策略默认）。
     */
    int startKeep() default -1;

    /**
     * 末尾保留位数（覆盖策略默认）。
     */
    int endKeep() default -1;

    /**
     * 替换符（覆盖策略默认）。
     */
    String replacement() default "";

    /**
     * 是否跳过脱敏。
     */
    boolean skip() default false;
}
