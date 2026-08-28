package cn.jowen.framework.i18n.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注 POJO 字段需国际化：拦截器在返回前将字段值视为消息编码解析替换。
 * 典型用于枚举/字典值返回文案（如状态码 → 状态文案）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface I18nField {

    /**
     * 消息编码前缀；为 {@code ""} 时字段原值即完整编码。
     * 例如 {@code codePrefix = "dict.status."} 时字段值 {@code ENABLED} 解析 {@code dict.status.ENABLED}。
     */
    String codePrefix() default "";
}
