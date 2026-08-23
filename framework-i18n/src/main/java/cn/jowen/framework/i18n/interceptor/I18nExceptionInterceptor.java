package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.annotation.I18nException;
import cn.jowen.framework.i18n.api.MessageSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 异常消息翻译工具：按异常类上的 {@link I18nException} 注解解析消息编码，
 * 将原始异常消息作为参数化入参产出最终文案。非 Servlet 组件，供全局异常处理器调用。
 *
 * <p>解析规则：取异常类及其父类链上首个携带 {@link I18nException} 注解的类型；
 * 编码未找到时回退为异常原始消息。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class I18nExceptionInterceptor {

    private final MessageSource messageSource;

    public I18nExceptionInterceptor(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private static @Nullable I18nException findAnnotation(Class<?> type) {
        Class<?> current = type;
        while (current != null && current != Object.class) {
            I18nException annotation = current.getAnnotation(I18nException.class);
            if (annotation != null) {
                return annotation;
            }
            current = current.getSuperclass();
        }
        return null;
    }

    /**
     * 解析异常的用户文案。
     *
     * @param throwable 异常，可为 {@code null}
     * @param locale    区域，不可为 {@code null}
     * @return 用户文案；异常为 {@code null} 返回 {@code null}
     */
    public @Nullable String resolve(@Nullable Throwable throwable, Locale locale) {
        if (throwable == null) {
            return null;
        }
        I18nException annotation = findAnnotation(throwable.getClass());
        if (annotation == null) {
            return throwable.getMessage();
        }
        String raw = messageSource.getMessage(annotation.value(), locale,
                new Object[]{throwable.getMessage()});
        return raw != null ? raw : throwable.getMessage();
    }
}
