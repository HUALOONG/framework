package cn.jowen.framework.extras.web.operatelog;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OperateLogAspectTest {

    @Mock
    private OperateLogHandler handler;
    @Mock
    private Executor executor;
    @Mock
    private OperatorProvider operatorProvider;
    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private MethodSignature signature;
    @Mock
    private OperateLog operateLog;

    private final Object target = new Object();

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(OperateLogAspectTest.class.getDeclaredMethod("sample", String.class, int.class));
        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getArgs()).thenReturn(new Object[]{"a", 1});
        when(operateLog.module()).thenReturn("order");
        when(operateLog.value()).thenReturn("");
        when(operateLog.recordParams()).thenReturn(false);
        when(operateLog.recordResult()).thenReturn(false);
        when(operatorProvider.currentOperator()).thenReturn("admin");
    }

    private String sample(String a, int b) {
        return "ok";
    }

    @Test
    void around_onSuccess_recordsEventAndReturnsResult() throws Throwable {
        when(pjp.proceed()).thenReturn("ok");

        OperateLogAspect aspect = new OperateLogAspect(handler, executor, false, operatorProvider);
        Object result = aspect.around(pjp, operateLog);

        assertThat(result).isEqualTo("ok");
        ArgumentCaptor<OperateLogEvent> captor = ArgumentCaptor.forClass(OperateLogEvent.class);
        verify(handler).handle(captor.capture());
        OperateLogEvent e = captor.getValue();
        assertThat(e.operator()).isEqualTo("admin");
        assertThat(e.module()).isEqualTo("order");
        assertThat(e.operation()).isEqualTo("sample");
        assertThat(e.method()).isEqualTo(target.getClass().getName() + "#sample");
        assertThat(e.success()).isTrue();
        assertThat(e.error()).isNull();
        assertThat(e.params()).isNull();
        assertThat(e.result()).isNull();
        assertThat(e.costMillis()).isGreaterThanOrEqualTo(0);
        assertThat(e.timestamp()).isNotNull();
        verify(executor, never()).execute(any(Runnable.class));
    }

    @Test
    void around_onFailure_recordsErrorAndRethrows() throws Throwable {
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        OperateLogAspect aspect = new OperateLogAspect(handler, executor, false, operatorProvider);

        assertThatThrownBy(() -> aspect.around(pjp, operateLog))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        ArgumentCaptor<OperateLogEvent> captor = ArgumentCaptor.forClass(OperateLogEvent.class);
        verify(handler).handle(captor.capture());
        OperateLogEvent e = captor.getValue();
        assertThat(e.success()).isFalse();
        assertThat(e.error()).isEqualTo("boom");
    }

    @Test
    void around_whenAsync_delegatesToExecutor() throws Throwable {
        when(pjp.proceed()).thenReturn("ok");

        OperateLogAspect aspect = new OperateLogAspect(handler, executor, true, operatorProvider);
        aspect.around(pjp, operateLog);

        verify(executor).execute(any(Runnable.class));
        verify(handler, never()).handle(any());
    }

    @Test
    void around_withExplicitValueAndRecordFlags_recordsSnapshot() throws Throwable {
        when(operateLog.value()).thenReturn("createOrder");
        when(operateLog.recordParams()).thenReturn(true);
        when(operateLog.recordResult()).thenReturn(true);
        when(pjp.proceed()).thenReturn("result-1");

        OperateLogAspect aspect = new OperateLogAspect(handler, executor, false, operatorProvider);
        aspect.around(pjp, operateLog);

        ArgumentCaptor<OperateLogEvent> captor = ArgumentCaptor.forClass(OperateLogEvent.class);
        verify(handler).handle(captor.capture());
        OperateLogEvent e = captor.getValue();
        assertThat(e.operation()).isEqualTo("createOrder");
        assertThat(e.params()).isEqualTo("[a, 1]");
        assertThat(e.result()).isEqualTo("result-1");
    }

    @Test
    void slf4jHandler_handlesEventWithoutError() {
        OperateLogHandler.Slf4j slf4j = new OperateLogHandler.Slf4j();
        OperateLogEvent event = new OperateLogEvent("admin", "order", "create", "X#y",
                null, null, true, null, 1L, Instant.now());
        slf4j.handle(event);
    }

    @Test
    void noneOperatorProvider_returnsNull() {
        assertThat(OperatorProvider.NONE.currentOperator()).isNull();
    }
}
