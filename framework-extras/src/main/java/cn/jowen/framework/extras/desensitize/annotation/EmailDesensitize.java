package cn.jowen.framework.extras.desensitize.annotation;

import cn.jowen.framework.extras.desensitize.serializer.DesensitizeJsonSerializer;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 邮箱脱敏注解（便捷封装），等价于元标注 {@link DesensitizeMeta#strategy() = "EMAIL"}，
 * 输出如 {@code a***@example.com}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@DesensitizeMeta(strategy = "EMAIL")
@JsonSerialize(using = DesensitizeJsonSerializer.class)
public @interface EmailDesensitize {

    /**
     * 是否跳过脱敏。
     */
    boolean skip() default false;
}
