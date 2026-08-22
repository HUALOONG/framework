package cn.jowen.framework.i18n.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/**
 * 标注方法返回值为国际化消息：切面/拦截器按 {@link #value()} 解析并替换返回值文案，
 * 支持参数化占位符（与 {@link #args()} 结合）。典型用于 RPC/Controller 返回错误码文案统一。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface I18nMessage {

    /** 消息编码。 */
    String value();

    /** 参数化参数（SPEL 表达式，按序对应占位符）。 */
    String[] args() default {};
}
