package cn.jowen.framework.cache.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedissonCacheLock} 测试。
 */
class RedissonCacheLockTest {

    private final RedissonClient client = mock(RedissonClient.class);
    private final RLock rlock = mock(RLock.class);
    private RedissonCacheLock lock;

    @BeforeEach
    void setUp() {
        when(client.getLock("lock-1")).thenReturn(rlock);
        lock = new RedissonCacheLock(client, "lock-1");
    }

    @Test
    void tryLock_withLeaseTime_delegatesWithLease() throws InterruptedException {
        when(rlock.tryLock(100, 500, TimeUnit.MILLISECONDS)).thenReturn(true);
        assertThat(lock.tryLock(Duration.ofMillis(100), Duration.ofMillis(500))).isTrue();
        verify(rlock).tryLock(100, 500, TimeUnit.MILLISECONDS);
    }

    @Test
    void tryLock_withoutLeaseTime_delegatesWithoutLease() throws InterruptedException {
        when(rlock.tryLock(100, TimeUnit.MILLISECONDS)).thenReturn(false);
        assertThat(lock.tryLock(Duration.ofMillis(100), null)).isFalse();
        verify(rlock).tryLock(100, TimeUnit.MILLISECONDS);
    }

    @Test
    void tryLock_interrupted_returnsFalseAndRestoresFlag() throws InterruptedException {
        when(rlock.tryLock(100, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("interrupted"));
        assertThat(lock.tryLock(Duration.ofMillis(100), null)).isFalse();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // 清理中断标志，避免影响后续测试
        Thread.interrupted();
    }

    @Test
    void unlock_whenHeldByCurrentThread_releasesLock() {
        when(rlock.isHeldByCurrentThread()).thenReturn(true);
        lock.unlock();
        verify(rlock).unlock();
    }

    @Test
    void unlock_whenNotHeld_doesNotRelease() {
        when(rlock.isHeldByCurrentThread()).thenReturn(false);
        lock.unlock();
        verify(rlock, never()).unlock();
    }

    @Test
    void isLocked_delegatesToRlock() {
        when(rlock.isLocked()).thenReturn(true);
        assertThat(lock.isLocked()).isTrue();
    }
}
