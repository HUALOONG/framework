package cn.jowen.framework.i18n.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/**
 * 标注异常类关联消息编码：异常处理器按 {@link #value()} 解析最终用户文案，
 * 避免异常消息硬编码。支持异常 {@link Throwable#getMessage()} 作为参数化入参。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface I18nException {

    /** 消息编码。 */
    String value();
}
