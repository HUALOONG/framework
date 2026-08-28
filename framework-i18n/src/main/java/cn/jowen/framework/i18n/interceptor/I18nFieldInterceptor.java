package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.annotation.I18nField;
import cn.jowen.framework.i18n.api.MessageSource;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 字段翻译工具：递归遍历对象树，将标注 {@link I18nField} 的字段值视为消息编码解析替换。
 * 非 Servlet 组件，供 AOP 切面 / 响应拦截器在返回链路调用。
 *
 * <p>处理范围：普通对象字段、{@link Collection} 元素、{@link Map} 值；
 * 字符串原样保留，嵌套对象递归处理，循环引用自动保护。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class I18nFieldInterceptor {

    private final MessageSource messageSource;

    public I18nFieldInterceptor(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    private static boolean isPrimitiveOrString(Object value) {
        Class<?> type = value.getClass();
        return type.isPrimitive() || value instanceof String || value instanceof Number
                || value instanceof Boolean || value instanceof Character
                || value instanceof java.util.Date || value instanceof Enum<?>;
    }

    /**
     * 翻译对象树中的标注字段。
     *
     * @param target 待翻译对象，可为 {@code null}
     * @param locale 区域，不可为 {@code null}
     */
    public void translate(@Nullable Object target, Locale locale) {
        translateInternal(target, locale, new IdentityHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private void translateInternal(@Nullable Object target, Locale locale, Map<Object, Boolean> visiting) {
        if (target == null || isPrimitiveOrString(target)) {
            return;
        }
        if (visiting.put(target, Boolean.TRUE) != null) {
            return; // 循环引用保护
        }
        if (target instanceof Collection<?> collection) {
            for (Object element : collection) {
                translateInternal(element, locale, visiting);
            }
            return;
        }
        if (target instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                translateInternal(entry.getValue(), locale, visiting);
            }
            return;
        }
        Class<?> type = target.getClass();
        while (type != null && type != Object.class) {
            for (Field field : type.getDeclaredFields()) {
                I18nField annotation = field.getAnnotation(I18nField.class);
                if (annotation == null) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value instanceof String code) {
                        String fullCode = annotation.codePrefix().isEmpty() ? code : annotation.codePrefix() + code;
                        String translated = messageSource.getMessage(fullCode, locale, null);
                        if (translated != null && !translated.equals(fullCode)) {
                            field.set(target, translated);
                        }
                    } else {
                        translateInternal(value, locale, visiting);
                    }
                } catch (IllegalAccessException ignored) {
                    // 不可访问字段跳过
                }
            }
            type = type.getSuperclass();
        }
    }
}
