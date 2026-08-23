package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.api.I18nContext;
import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 请求区域拦截器（Servlet Filter）：解析请求区域并绑定到 {@link I18nContext}，
 * 链路结束后自动清理。区域解析失败时保持系统默认，不阻断请求。
 *
 * <p>典型配置：{@code ParameterLocaleResolver → CookieLocaleResolver → AcceptHeaderLocaleResolver}
 * 组合解析，本过滤器作为最先执行的 Filter 注册。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public class I18nInterceptor implements Filter {

    private final LocaleResolver resolver;

    public I18nInterceptor(LocaleResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        Locale locale = resolveLocale(request);
        if (locale == null) {
            chain.doFilter(request, response);
            return;
        }
        AtomicReference<Exception> failure = new AtomicReference<>();
        I18nContext.withLocale(locale, () -> {
            try {
                chain.doFilter(request, response);
            } catch (IOException | ServletException ex) {
                failure.set(ex);
            }
            return null;
        });
        Exception ex = failure.get();
        if (ex instanceof IOException io) {
            throw io;
        }
        if (ex instanceof ServletException se) {
            throw se;
        }
    }

    private @Nullable Locale resolveLocale(ServletRequest request) {
        if (request instanceof HttpServletRequest http) {
            return resolver.resolve(http);
        }
        return null;
    }
}
