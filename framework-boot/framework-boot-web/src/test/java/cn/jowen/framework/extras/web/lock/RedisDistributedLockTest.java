package cn.jowen.framework.extras.web.lock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisDistributedLockTest {

    @Mock
    private RedisCommandExecutor executor;

    @Test
    void acquire_withNonPositiveLease_usesDefaultLease() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:1", 100L, 0L);

        when(executor.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);
        handle.lock();

        verify(executor).setIfAbsent(eq("order:1"), anyString(), eq(3000L));
    }

    @Test
    void acquire_withPositiveLease_usesGivenLease() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:2", 100L, 5000L);

        when(executor.setIfAbsent(anyString(), anyString(), anyLong())).thenReturn(true);
        handle.lock();

        verify(executor).setIfAbsent(eq("order:2"), anyString(), eq(5000L));
    }

    @Test
    void lock_whenSetIfAbsentTrue_returnsImmediately() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:3", 1000L, 3000L);

        when(executor.setIfAbsent(eq("order:3"), anyString(), eq(3000L))).thenReturn(true);
        handle.lock();

        verify(executor, times(1)).setIfAbsent(eq("order:3"), anyString(), eq(3000L));
    }

    @Test
    void lock_whenContended_retriesUntilDeadline() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:4", 1000L, 3000L);

        when(executor.setIfAbsent(eq("order:4"), anyString(), eq(3000L)))
                .thenReturn(false)
                .thenReturn(true);
        handle.lock();

        verify(executor, times(2)).setIfAbsent(eq("order:4"), anyString(), eq(3000L));
    }

    @Test
    void lock_whenTimedOut_throwsLockAcquireException() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:5", 0L, 3000L);

        when(executor.setIfAbsent(eq("order:5"), anyString(), eq(3000L))).thenReturn(false);

        assertThatThrownBy(handle::lock)
                .isInstanceOf(LockAcquireException.class)
                .hasMessageContaining("order:5");
    }

    @Test
    void close_releasesViaDeleteIfMatch() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:6", 100L, 3000L);

        when(executor.setIfAbsent(eq("order:6"), anyString(), eq(3000L))).thenReturn(true);
        handle.lock();
        handle.close();

        verify(executor).deleteIfMatch(eq("order:6"), anyString());
    }

    @Test
    void close_alwaysInvokesDeleteIfMatch() {
        RedisDistributedLock lock = new RedisDistributedLock(executor, 3000L);
        Lock handle = lock.acquire("order:7", 100L, 3000L);

        handle.close();

        verify(executor).deleteIfMatch(eq("order:7"), anyString());
    }

    @Test
    void lockType_hasExpectedValues() {
        assertThat(LockType.values()).containsExactly(LockType.LOCAL, LockType.REDIS);
        assertThat(LockType.valueOf("LOCAL")).isEqualTo(LockType.LOCAL);
        assertThat(LockType.valueOf("REDIS")).isEqualTo(LockType.REDIS);
    }
}
