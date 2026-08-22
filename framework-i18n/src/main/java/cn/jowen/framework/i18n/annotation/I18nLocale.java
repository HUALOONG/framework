package cn.jowen.framework.i18n.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jspecify.annotations.NullMarked;

/**
 * 标注请求处理方法参数为区域（{@link java.util.Locale} 或 {@code String} 语言标签）：
 * 拦截器将该参数值绑定为当前请求区域。典型用于无需协商/参数的显式区域传参。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface I18nLocale {
}
