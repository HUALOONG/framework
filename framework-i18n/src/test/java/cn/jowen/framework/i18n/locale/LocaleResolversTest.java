package cn.jowen.framework.i18n.locale;

import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 各 {@link LocaleResolver} 实现的单元测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@ExtendWith(MockitoExtension.class)
class LocaleResolversTest {

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    // ===================== FixedLocaleResolver =====================

    @Test
    void fixed_alwaysReturnsConfiguredLocale() {
        FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.FRENCH);
        assertThat(resolver.resolve(request)).isEqualTo(Locale.FRENCH);
    }

    @Test
    void fixed_nullContext_returnsLocale() {
        FixedLocaleResolver resolver = new FixedLocaleResolver(Locale.CHINESE);
        assertThat(resolver.resolve(null)).isEqualTo(Locale.CHINESE);
    }

    @Test
    void fixed_nullLocale_throws() {
        assertThatThrownBy(() -> new FixedLocaleResolver(null)).isInstanceOf(NullPointerException.class);
    }

    // ===================== ParameterLocaleResolver =====================

    @Test
    void parameter_fromRequestParam() {
        when(request.getParameter("lang")).thenReturn("zh-CN");
        ParameterLocaleResolver resolver = new ParameterLocaleResolver("lang");
        assertThat(resolver.resolve(request)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void parameter_defaultParamName() {
        when(request.getParameter(ParameterLocaleResolver.DEFAULT_PARAM)).thenReturn("en-US");
        ParameterLocaleResolver resolver = new ParameterLocaleResolver();
        assertThat(resolver.resolve(request)).isEqualTo(Locale.US);
    }

    @Test
    void parameter_nullParam_returnsNull() {
        when(request.getParameter("lang")).thenReturn(null);
        ParameterLocaleResolver resolver = new ParameterLocaleResolver("lang");
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void parameter_nullContext_returnsNull() {
        ParameterLocaleResolver resolver = new ParameterLocaleResolver("lang");
        assertThat(resolver.resolve(null)).isNull();
    }

    // ===================== CookieLocaleResolver =====================

    @Test
    void cookie_present() {
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("LOCALE", "fr-FR")
        });
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");
        assertThat(resolver.resolve(request)).isEqualTo(Locale.FRANCE);
    }

    @Test
    void cookie_missing() {
        when(request.getCookies()).thenReturn(new Cookie[0]);
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void cookie_noCookies() {
        when(request.getCookies()).thenReturn(null);
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void cookie_defaultName() {
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie(CookieLocaleResolver.DEFAULT_COOKIE, "de-DE")
        });
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        assertThat(resolver.resolve(request)).isEqualTo(Locale.GERMANY);
    }

    @Test
    void cookie_nullContext_returnsNull() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("LOCALE");
        assertThat(resolver.resolve(null)).isNull();
    }

    // ===================== SessionLocaleResolver =====================

    @Test
    void session_presentAsLocale() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionLocaleResolver.DEFAULT_ATTRIBUTE)).thenReturn(Locale.JAPANESE);
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        assertThat(resolver.resolve(request)).isEqualTo(Locale.JAPANESE);
    }

    @Test
    void session_presentAsString() {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionLocaleResolver.DEFAULT_ATTRIBUTE)).thenReturn("en-GB");
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        assertThat(resolver.resolve(request)).isEqualTo(Locale.UK);
    }

    @Test
    void session_noSession() {
        when(request.getSession(false)).thenReturn(null);
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void session_nullContext_returnsNull() {
        SessionLocaleResolver resolver = new SessionLocaleResolver();
        assertThat(resolver.resolve(null)).isNull();
    }

    // ===================== AcceptHeaderLocaleResolver =====================

    @Test
    void acceptHeader_firstPreferred() {
        when(request.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9,zh-CN;q=0.8");
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        assertThat(resolver.resolve(request)).isEqualTo(Locale.US);
    }

    @Test
    void acceptHeader_matchesSupported() {
        when(request.getHeader("Accept-Language")).thenReturn("en-US,zh-CN;q=0.9");
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver(List.of(Locale.SIMPLIFIED_CHINESE));
        assertThat(resolver.resolve(request)).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void acceptHeader_emptyHeader_returnsNull() {
        when(request.getHeader("Accept-Language")).thenReturn(null);
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        assertThat(resolver.resolve(request)).isNull();
    }

    @Test
    void acceptHeader_nullContext_returnsNull() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        assertThat(resolver.resolve(null)).isNull();
    }

    // ===================== CompositeLocaleResolver =====================

    @Test
    void composite_firstResolvedWins() {
        LocaleResolver a = new FixedLocaleResolver(Locale.FRENCH);
        LocaleResolver b = new FixedLocaleResolver(Locale.GERMAN);
        CompositeLocaleResolver composite = new CompositeLocaleResolver(List.of(a, b));
        assertThat(composite.resolve(request)).isEqualTo(Locale.FRENCH);
    }

    @Test
    void composite_skipsNullThenResolves() {
        LocaleResolver nullResolver = ctx -> null;
        LocaleResolver real = new FixedLocaleResolver(Locale.CHINESE);
        CompositeLocaleResolver composite = new CompositeLocaleResolver(List.of(nullResolver, real));
        assertThat(composite.resolve(request)).isEqualTo(Locale.CHINESE);
    }

    @Test
    void composite_allNull_returnsNull() {
        CompositeLocaleResolver composite = new CompositeLocaleResolver(List.of(ctx -> null, ctx -> null));
        assertThat(composite.resolve(request)).isNull();
    }

    @Test
    void composite_nullContext_passedThrough() {
        LocaleResolver a = new FixedLocaleResolver(Locale.US);
        CompositeLocaleResolver composite = new CompositeLocaleResolver(List.of(a));
        assertThat(composite.resolve(null)).isEqualTo(Locale.US);
    }
}
