package cn.jowen.framework.extras.web.idempotent;

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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IdempotentAspectTest {

    @Mock
    private IdempotentStore store;
    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private MethodSignature signature;
    @Mock
    private Idempotent idempotent;

    private final Object target = new Object();

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(IdempotentAspectTest.class.getDeclaredMethod("sample", String.class));
        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getArgs()).thenReturn(new Object[]{"u1"});
        when(idempotent.key()).thenReturn("");
        when(idempotent.expire()).thenReturn(300);
        when(idempotent.message()).thenReturn("请勿重复提交");
    }

    private void sample(String userId) {
    }

    @Test
    void around_firstCall_marksProceedsAndRemoves() throws Throwable {
        when(store.tryMark("idem:" + target.getClass().getName() + ".sample", 300L, TimeUnit.SECONDS))
                .thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        IdempotentAspect aspect = new IdempotentAspect(store);
        Object result = aspect.around(pjp, idempotent);

        assertThat(result).isEqualTo("ok");
        verify(store).tryMark(any(String.class), eq(300L), eq(TimeUnit.SECONDS));
        verify(store).remove(any(String.class));
    }

    @Test
    void around_duplicateCall_throwsWithoutProceeding() throws Throwable {
        when(store.tryMark(any(String.class), eq(300L), eq(TimeUnit.SECONDS))).thenReturn(false);

        IdempotentAspect aspect = new IdempotentAspect(store);

        assertThatThrownBy(() -> aspect.around(pjp, idempotent))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("请勿重复提交");
        verify(pjp, never()).proceed();
        verify(store, never()).remove(any(String.class));
    }

    @Test
    void around_whenProceedThrows_removesFingerprint() throws Throwable {
        when(store.tryMark(any(String.class), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        IdempotentAspect aspect = new IdempotentAspect(store);

        assertThatThrownBy(() -> aspect.around(pjp, idempotent))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        verify(store).remove(any(String.class));
    }

    @Test
    void around_withSpelKey_resolvesKey() throws Throwable {
        when(idempotent.key()).thenReturn("#p0");
        when(store.tryMark("idem:u1", 300L, TimeUnit.SECONDS)).thenReturn(true);
        when(pjp.proceed()).thenReturn("ok");

        IdempotentAspect aspect = new IdempotentAspect(store);
        aspect.around(pjp, idempotent);

        verify(store).tryMark("idem:u1", 300L, TimeUnit.SECONDS);
    }
}
