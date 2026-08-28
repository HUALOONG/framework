package cn.jowen.framework.i18n.api;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 消息源。零耦合的国际化消息解析抽象，不依赖 Servlet 或 Spring。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface MessageSource {

    /**
     * 解析消息。
     *
     * @param code   消息键，不可为 {@code null}
     * @param locale 区域，不可为 {@code null}
     * @param args   格式化参数，可为 {@code null}
     * @return 格式化后的消息；未找到返回 {@code null}
     */
    @Nullable String getMessage(String code, Locale locale, @Nullable Object @Nullable [] args);

    /**
     * 是否包含指定键。
     *
     * @param code   消息键，不可为 {@code null}
     * @param locale 区域，不可为 {@code null}
     * @return 包含返回 {@code true}
     */
    boolean contains(String code, Locale locale);

    /**
     * 便捷方法：使用 {@link LocaleContextHolder} 当前区域解析。
     *
     * @param code 消息键，不可为 {@code null}
     * @param args 格式化参数，可为 {@code null}
     * @return 消息或 {@code null}
     */
    default @Nullable String getMessage(String code, @Nullable Object @Nullable [] args) {
        return getMessage(code, LocaleContextHolder.getLocale(), args);
    }

    /**
     * 按可解析引用解析消息。
     *
     * @param resolvable 消息引用（含编码/参数/默认文案），不可为 {@code null}
     * @param locale     区域，不可为 {@code null}
     * @return 格式化后的消息；未找到且无默认文案时返回 {@code null}
     */
    default @Nullable String getMessage(MessageSourceResolvable resolvable, Locale locale) {
        String message = getMessage(resolvable.code(), locale, resolvable.args());
        return message != null ? message : resolvable.defaultMessage();
    }

    /**
     * 严格解析：未找到时抛 {@link MessageNotFoundException}。
     *
     * @param code   消息键，不可为 {@code null}
     * @param locale 区域，不可为 {@code null}
     * @param args   格式化参数，可为 {@code null}
     * @return 格式化后的消息，永不为 {@code null}
     */
    default String getMessageRequired(String code, Locale locale, @Nullable Object @Nullable [] args) {
        String message = getMessage(code, locale, args);
        if (message == null) {
            throw new MessageNotFoundException(code);
        }
        return message;
    }
}
