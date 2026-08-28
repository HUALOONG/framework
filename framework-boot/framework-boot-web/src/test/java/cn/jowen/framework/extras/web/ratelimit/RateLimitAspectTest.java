package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitAspectTest {

    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private MethodSignature signature;
    @Mock
    private RateLimit rateLimit;

    private final Object target = new Object();

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(RateLimitAspectTest.class.getDeclaredMethod("sample"));
        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(rateLimit.key()).thenReturn("");
        when(rateLimit.permits()).thenReturn(10);
        when(rateLimit.window()).thenReturn(60);
        when(rateLimit.message()).thenReturn("请求过于频繁");
    }

    private void sample() {
    }

    @Test
    void around_whenPermitAvailable_proceeds() throws Throwable {
        when(pjp.proceed()).thenReturn("ok");

        RateLimitAspect aspect = new RateLimitAspect(new RateLimiterManager());
        Object result = aspect.around(pjp, rateLimit);

        assertThat(result).isEqualTo("ok");
        verify(pjp).proceed();
    }

    @Test
    void around_whenPermitsExhausted_throwsExtrasException() throws Throwable {
        when(rateLimit.permits()).thenReturn(1);
        when(pjp.proceed()).thenReturn("ok");

        RateLimitAspect aspect = new RateLimitAspect(new RateLimiterManager());
        assertThat(aspect.around(pjp, rateLimit)).isEqualTo("ok");

        assertThatThrownBy(() -> aspect.around(pjp, rateLimit))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("请求过于频繁");
    }

    @Test
    void around_withCustomMessage_usesConfiguredMessage() throws Throwable {
        when(rateLimit.permits()).thenReturn(1);
        when(rateLimit.message()).thenReturn("too many requests");
        when(pjp.proceed()).thenReturn("ok");

        RateLimitAspect aspect = new RateLimitAspect(new RateLimiterManager());
        aspect.around(pjp, rateLimit);

        assertThatThrownBy(() -> aspect.around(pjp, rateLimit))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("too many requests");
    }
}
