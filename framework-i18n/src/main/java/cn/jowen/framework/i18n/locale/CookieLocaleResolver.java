package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * Cookie 区域解析器：从 Cookie（默认 {@code locale}，值如 {@code zh-CN}）解析区域。
 * 上下文须为 {@link HttpServletRequest}；Cookie 缺失或非法返回 {@code null}。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class CookieLocaleResolver implements LocaleResolver {

    public static final String DEFAULT_COOKIE = "locale";

    private final String cookieName;

    public CookieLocaleResolver() {
        this(DEFAULT_COOKIE);
    }

    /**
     * 构造解析器。
     *
     * @param cookieName Cookie 名，不可为 {@code null}
     */
    public CookieLocaleResolver(String cookieName) {
        this.cookieName = cookieName != null ? cookieName : DEFAULT_COOKIE;
    }

    @Override
    public @Nullable Locale resolve(@Nullable Object context) {
        if (!(context instanceof HttpServletRequest request)) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieName.equals(cookie.getName())) {
                return LocaleUtils.parseTag(cookie.getValue());
            }
        }
        return null;
    }
}
