package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.api.I18nContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import org.jspecify.annotations.NullMarked;

/**
 * 响应语言头拦截器（Servlet Filter）：在响应上写入 {@code Content-Language}
 * 头（当前请求区域），供前端按语言渲染。应注册于 {@link I18nInterceptor} 之后。
 *
 * <p>区域取自 {@link I18nContext#getCurrentLocale()}，
 * 无请求区域时写入系统默认语言标签。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public class I18nResponseInterceptor implements Filter {

    /** 响应语言头名称。 */
    public static final String CONTENT_LANGUAGE = "Content-Language";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (response instanceof HttpServletResponse http) {
            Locale locale = I18nContext.getCurrentLocale();
            http.setHeader(CONTENT_LANGUAGE, locale.toLanguageTag());
        }
        chain.doFilter(request, response);
    }
}
