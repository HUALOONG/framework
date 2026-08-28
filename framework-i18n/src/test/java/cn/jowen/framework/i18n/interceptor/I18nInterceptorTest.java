package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.api.I18nContext;
import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link I18nInterceptor} 测试。
 */
class I18nInterceptorTest {

    private final LocaleResolver resolver = mock(LocaleResolver.class);
    private final I18nInterceptor interceptor = new I18nInterceptor(resolver);

    @Test
    void doFilter_nonHttpRequest_passesThrough() throws IOException, ServletException {
        ServletRequest request = mock(ServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        interceptor.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_bindsLocaleAroundChain() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        when(resolver.resolve(request)).thenReturn(Locale.CHINA);
        AtomicReference<Locale> seen = new AtomicReference<>();
        FilterChain chain = (req, resp) -> seen.set(I18nContext.getCurrentLocale());

        interceptor.doFilter(request, response, chain);

        assertThat(seen.get()).isEqualTo(Locale.CHINA);
        // 链路结束后区域清理为默认
        assertThat(I18nContext.getCurrentLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void doFilter_resolverNull_passesThrough() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        when(resolver.resolve(request)).thenReturn(null);
        FilterChain chain = mock(FilterChain.class);

        interceptor.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_chainThrowsIOException_propagates() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        when(resolver.resolve(request)).thenReturn(Locale.US);
        FilterChain chain = mock(FilterChain.class);
        IOException boom = new IOException("boom");
        doThrow(boom).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> interceptor.doFilter(request, response, chain))
                .isSameAs(boom);
    }

    @Test
    void doFilter_chainThrowsServletException_propagates() throws IOException, ServletException {
        HttpServletRequest request = mock(HttpServletRequest.class);
        ServletResponse response = mock(ServletResponse.class);
        when(resolver.resolve(request)).thenReturn(Locale.US);
        FilterChain chain = mock(FilterChain.class);
        ServletException boom = new ServletException("boom");
        doThrow(boom).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> interceptor.doFilter(request, response, chain))
                .isSameAs(boom);
    }
}
