package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.api.I18nContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link I18nResponseInterceptor} 测试。
 */
class I18nResponseInterceptorTest {

    private final I18nResponseInterceptor interceptor = new I18nResponseInterceptor();

    @Test
    void doFilter_writesContentLanguage() throws IOException, ServletException {
        ServletRequest request = mock(ServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        I18nContext.withLocale(Locale.CHINA, () -> {
            try {
                interceptor.doFilter(request, response, chain);
            } catch (IOException | ServletException e) {
                throw new RuntimeException(e);
            }
            return null;
        });

        verify(response).setHeader(I18nResponseInterceptor.CONTENT_LANGUAGE, "zh-CN");
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_withoutLocale_usesDefault() throws IOException, ServletException {
        ServletRequest request = mock(ServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        interceptor.doFilter(request, response, chain);

        verify(response).setHeader(I18nResponseInterceptor.CONTENT_LANGUAGE, Locale.getDefault().toLanguageTag());
    }

    @Test
    void doFilter_nonHttpResponse_skipsHeader() throws IOException, ServletException {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        interceptor.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response).isNotInstanceOf(HttpServletResponse.class);
    }

    @Test
    void contentLanguage_constant() {
        assertThat(I18nResponseInterceptor.CONTENT_LANGUAGE).isEqualTo("Content-Language");
    }
}
