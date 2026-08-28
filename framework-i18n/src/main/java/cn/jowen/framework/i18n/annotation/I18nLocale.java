package cn.jowen.framework.i18n.annotation;

import org.jspecify.annotations.NullMarked;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标注请求处理方法参数为区域（{@link java.util.Locale} 或 {@code String} 语言标签）：
 * 拦截器将该参数值绑定为当前请求区域。典型用于无需协商/参数的显式区域传参。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface I18nLocale {
}
