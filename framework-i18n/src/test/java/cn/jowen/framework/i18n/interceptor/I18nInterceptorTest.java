package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.api.I18nContext;
import cn.jowen.framework.i18n.api.LocaleResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Proxy;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class I18nInterceptorTest {

    @Test
    void bindsResolvedLocaleDuringChain() throws Exception {
        LocaleResolver resolver = context -> new Locale("zh", "CN");
        I18nInterceptor interceptor = new I18nInterceptor(resolver);
        HttpServletRequest request = stubRequest();
        FilterChain chain = (req, res) ->
                assertThat(I18nContext.getCurrentLocale()).isEqualTo(new Locale("zh", "CN"));
        interceptor.doFilter(request, null, chain);
        // 链路外恢复默认
        assertThat(I18nContext.getCurrentLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void skipsBindingWhenResolutionFails() throws Exception {
        LocaleResolver resolver = context -> null;
        I18nInterceptor interceptor = new I18nInterceptor(resolver);
        FilterChain chain = (req, res) ->
                assertThat(I18nContext.getCurrentLocale()).isEqualTo(Locale.getDefault());
        interceptor.doFilter(stubRequest(), null, chain);
    }

    private static HttpServletRequest stubRequest() {
        return (HttpServletRequest) Proxy.newProxyInstance(
                I18nInterceptorTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getHeader" -> "zh-CN";
                    default -> null;
                });
    }
}
