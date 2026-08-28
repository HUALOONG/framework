package cn.jowen.framework.extras.web.lock;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LockAspectTest {

    @Mock
    private DistributedLock distributedLock;
    @Mock
    private Lock distributed;
    @Mock
    private ProceedingJoinPoint pjp;
    @Mock
    private MethodSignature signature;
    @Mock
    private Lockable lockable;

    private final Object target = new Object();

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(LockAspectTest.class.getDeclaredMethod("sample"));
        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getArgs()).thenReturn(new Object[0]);
        when(lockable.key()).thenReturn("");
    }

    private void sample() {
    }

    @Test
    void around_withLocalType_proceedsWithLocalLock() throws Throwable {
        when(lockable.type()).thenReturn(LockType.LOCAL);
        when(lockable.waitMillis()).thenReturn(100L);
        when(pjp.proceed()).thenReturn("ok");

        LockAspect aspect = new LockAspect(distributedLock);
        Object result = aspect.around(pjp, lockable);

        assertThat(result).isEqualTo("ok");
        verify(distributedLock, never()).acquire(anyString(), anyLong(), anyLong());
    }

    @Test
    void around_withRedisType_usesDistributedLock() throws Throwable {
        when(lockable.type()).thenReturn(LockType.REDIS);
        when(lockable.waitMillis()).thenReturn(100L);
        when(lockable.leaseMillis()).thenReturn(3000L);
        when(distributedLock.acquire("lock:" + target.getClass().getName() + ".sample", 100L, 3000L))
                .thenReturn(distributed);
        when(pjp.proceed()).thenReturn("ok");

        LockAspect aspect = new LockAspect(distributedLock);
        Object result = aspect.around(pjp, lockable);

        assertThat(result).isEqualTo("ok");
        verify(distributed).lock();
        verify(distributed).close();
    }

    @Test
    void around_withRedisTypeButNoDistributedLock_fallsBackToLocal() throws Throwable {
        when(lockable.type()).thenReturn(LockType.REDIS);
        when(lockable.waitMillis()).thenReturn(100L);
        when(pjp.proceed()).thenReturn("ok");

        LockAspect aspect = new LockAspect(null);
        Object result = aspect.around(pjp, lockable);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void around_whenProceedThrows_stillReleasesLock() throws Throwable {
        when(lockable.type()).thenReturn(LockType.REDIS);
        when(lockable.waitMillis()).thenReturn(100L);
        when(lockable.leaseMillis()).thenReturn(3000L);
        when(distributedLock.acquire(anyString(), anyLong(), anyLong())).thenReturn(distributed);
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        LockAspect aspect = new LockAspect(distributedLock);

        assertThatThrownBy(() -> aspect.around(pjp, lockable))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        verify(distributed).lock();
        verify(distributed).close();
    }
}
