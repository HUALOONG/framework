package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * 会话区域解析器：从 HttpSession（属性名默认 {@code locale}）解析区域。
 * 上下文须为 {@link HttpServletRequest}；会话或属性缺失返回 {@code null}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SessionLocaleResolver implements LocaleResolver {

    public static final String DEFAULT_ATTRIBUTE = "locale";

    private final String attributeName;

    public SessionLocaleResolver() {
        this(DEFAULT_ATTRIBUTE);
    }

    /**
     * 构造解析器。
     *
     * @param attributeName 会话属性名，不可为 {@code null}
     */
    public SessionLocaleResolver(String attributeName) {
        this.attributeName = attributeName != null ? attributeName : DEFAULT_ATTRIBUTE;
    }

    @Override
    public @Nullable Locale resolve(@Nullable Object context) {
        if (!(context instanceof HttpServletRequest request)) {
            return null;
        }
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(attributeName);
        if (value instanceof Locale locale) {
            return locale;
        }
        return LocaleUtils.parseTag(value != null ? value.toString() : null);
    }
}
