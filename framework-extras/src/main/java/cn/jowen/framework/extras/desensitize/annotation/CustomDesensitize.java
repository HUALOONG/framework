package cn.jowen.framework.extras.desensitize.annotation;

import cn.jowen.framework.extras.desensitize.serializer.DesensitizeJsonSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义脱敏注解（便捷封装），等价于元标注 {@link DesensitizeMeta#strategy() = "CUSTOM"} 并携带保留位/替换符。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@DesensitizeMeta(strategy = "CUSTOM")
@JsonSerialize(using = DesensitizeJsonSerializer.class)
public @interface CustomDesensitize {

    /**
     * 开头保留位数（>=0 覆盖策略默认）。
     */
    int startKeep() default -1;

    /**
     * 末尾保留位数（>=0 覆盖策略默认）。
     */
    int endKeep() default -1;

    /**
     * 替换符（非空覆盖策略默认）。
     */
    String replacement() default "";

    /**
     * 是否跳过脱敏。
     */
    boolean skip() default false;
}
