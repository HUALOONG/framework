package cn.jowen.framework.extras.web.sign;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.method.HandlerMethod;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SignInterceptorTest {

    @Mock
    private SignVerifier verifier;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private HandlerMethod handlerMethod;
    @Mock
    private Sign sign;

    @BeforeEach
    void setUp() {
        when(sign.header()).thenReturn("X-Signature");
        when(sign.timestampHeader()).thenReturn("X-Timestamp");
        when(sign.toleranceSeconds()).thenReturn(300);
        when(sign.message()).thenReturn("签名校验失败");
        when(sign.fields()).thenReturn(new String[0]);
    }

    private SignInterceptor interceptor() {
        return new SignInterceptor(verifier);
    }

    @Test
    void preHandle_nonHandlerMethod_returnsTrue() {
        assertThat(interceptor().preHandle(request, response, new Object())).isTrue();
    }

    @Test
    void preHandle_withoutAnyAnnotation_returnsTrue() {
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(null);
        doReturn(SignInterceptorTest.class).when(handlerMethod).getBeanType();

        assertThat(interceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @Test
    void preHandle_classLevelAnnotation_isResolved() {
        long now = System.currentTimeMillis();
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(null);
        doReturn(SignedController.class).when(handlerMethod).getBeanType();
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));
        when(request.getHeader("X-Signature")).thenReturn("sig");
        when(request.getHeader("X-App-Id")).thenReturn(null);
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        when(verifier.verify("", "", "sig", now)).thenReturn(true);

        assertThat(interceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @Test
    void preHandle_missingTimestamp_throws() {
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(null);

        assertThatThrownBy(() -> interceptor().preHandle(request, response, handlerMethod))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("请求时间戳超出容忍范围");
    }

    @Test
    void preHandle_invalidTimestamp_throws() {
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn("not-a-number");

        assertThatThrownBy(() -> interceptor().preHandle(request, response, handlerMethod))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("请求时间戳超出容忍范围");
    }

    @Test
    void preHandle_timestampOutOfTolerance_throws() {
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp"))
                .thenReturn(String.valueOf(System.currentTimeMillis() + 301_000L));

        assertThatThrownBy(() -> interceptor().preHandle(request, response, handlerMethod))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("请求时间戳超出容忍范围");
    }

    @Test
    void preHandle_missingSignatureHeader_throws() {
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
        when(request.getHeader("X-Signature")).thenReturn(null);

        assertThatThrownBy(() -> interceptor().preHandle(request, response, handlerMethod))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("签名校验失败");
    }

    @Test
    void preHandle_blankSignature_throws() {
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(System.currentTimeMillis()));
        when(request.getHeader("X-Signature")).thenReturn("  ");

        assertThatThrownBy(() -> interceptor().preHandle(request, response, handlerMethod))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("签名校验失败");
    }

    @Test
    void preHandle_whenVerifyFails_throws() {
        long now = System.currentTimeMillis();
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));
        when(request.getHeader("X-Signature")).thenReturn("sig");
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        when(verifier.verify("", "", "sig", now)).thenReturn(false);

        assertThatThrownBy(() -> interceptor().preHandle(request, response, handlerMethod))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("签名校验失败");
    }

    @Test
    void preHandle_whenVerifySucceeds_returnsTrue() {
        long now = System.currentTimeMillis();
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));
        when(request.getHeader("X-Signature")).thenReturn("sig");
        when(request.getHeader("X-App-Id")).thenReturn("app1");
        when(request.getParameterNames()).thenReturn(Collections.emptyEnumeration());
        when(verifier.verify("app1", "", "sig", now)).thenReturn(true);

        assertThat(interceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @Test
    void preHandle_withAllParameters_payloadConcatenatesAllParams() {
        long now = System.currentTimeMillis();
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));
        when(request.getHeader("X-Signature")).thenReturn("sig");
        when(request.getParameter("a")).thenReturn("1");
        when(request.getParameter("b")).thenReturn("2");
        when(request.getParameterNames()).thenReturn(Collections.enumeration(List.of("a", "b")));
        when(verifier.verify(anyString(), eq("a=1&b=2&"), eq("sig"), eq(now))).thenReturn(true);

        assertThat(interceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @Test
    void preHandle_withExplicitFields_usesConfiguredOrder() {
        when(sign.fields()).thenReturn(new String[]{"b", "a"});
        long now = System.currentTimeMillis();
        when(handlerMethod.getMethodAnnotation(Sign.class)).thenReturn(sign);
        when(request.getHeader("X-Timestamp")).thenReturn(String.valueOf(now));
        when(request.getHeader("X-Signature")).thenReturn("sig");
        when(request.getParameter("b")).thenReturn("2");
        when(request.getParameter("a")).thenReturn("1");
        when(verifier.verify(anyString(), eq("b=2&a=1&"), eq("sig"), eq(now))).thenReturn(true);

        assertThat(interceptor().preHandle(request, response, handlerMethod)).isTrue();
    }

    @Sign
    static class SignedController {
    }
}
