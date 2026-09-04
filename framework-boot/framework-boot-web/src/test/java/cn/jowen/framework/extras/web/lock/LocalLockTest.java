package cn.jowen.framework.extras.web.lock;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LocalLock} 测试。
 *
 * <p>覆盖：加锁/释放、try-with-resources、同 key 跨线程互斥、等待超时抛
 * {@link LockAcquireException}、可重入语义、close 幂等、不同 key 互不阻塞、
 * 静态锁表按 key 共享同一底层锁。
 *
 * <p>注意：{@code LocalLock} 内部使用静态锁表，测试之间必须使用互不相同的 key。
 */
class LocalLockTest {

    private static String uniqueKey() {
        return "test-lock:" + UUID.randomUUID();
    }

    // ---------- 基本加解锁 ----------

    @Test
    void lock_thenClose_releasesLock() {
        String key = uniqueKey();

        LocalLock first = new LocalLock(key, 0);
        first.lock();
        first.close();

        LocalLock second = new LocalLock(key, 0);
        assertThatCode(second::lock).doesNotThrowAnyException();
        second.close();
    }

    @Test
    void lock_worksWithTryWithResources() {
        String key = uniqueKey();

        try (LocalLock lock = new LocalLock(key, 0)) {
            lock.lock();
        }

        try (LocalLock again = new LocalLock(key, 0)) {
            assertThatCode(again::lock).doesNotThrowAnyException();
        }
    }

    @Test
    void close_isIdempotent() {
        String key = uniqueKey();
        LocalLock lock = new LocalLock(key, 0);
        lock.lock();

        lock.close();
        assertThatCode(lock::close).as("重复 close 不应抛异常").doesNotThrowAnyException();

        LocalLock other = new LocalLock(key, 0);
        assertThatCode(other::lock).doesNotThrowAnyException();
        other.close();
    }

    @Test
    void close_withoutLock_isNoop() {
        LocalLock lock = new LocalLock(uniqueKey(), 0);

        assertThatCode(lock::close).doesNotThrowAnyException();
    }

    @Test
    void implementsLockContract() {
        LocalLock lock = new LocalLock(uniqueKey(), 0);
        assertThat(lock).isInstanceOf(Lock.class).isInstanceOf(AutoCloseable.class);
    }

    // ---------- 可重入 ----------

    @Test
    void lock_isReentrantForSameThread() {
        String key = uniqueKey();
        LocalLock lock = new LocalLock(key, 0);

        lock.lock();
        assertThatCode(lock::lock).as("同线程重入不应失败").doesNotThrowAnyException();

        // 重入计数为 2，单次 close 只减一次；需再次 close 才彻底释放
        lock.close();
        lock.close();

        LocalLock other = new LocalLock(key, 0);
        assertThatCode(other::lock).doesNotThrowAnyException();
        other.close();
    }

    @Test
    void separateInstancesOnSameKeyShareUnderlyingLock() {
        String key = uniqueKey();
        LocalLock a = new LocalLock(key, 0);
        LocalLock b = new LocalLock(key, 0);

        a.lock();
        // 同线程 -> 共享的 ReentrantLock 允许重入，因此 b 也能获取
        assertThatCode(b::lock).doesNotThrowAnyException();

        b.close();
        a.close();
    }

    // ---------- 跨线程互斥 ----------

    @Test
    void lock_blocksOtherThreadAndFailsFastWithZeroWait() throws Exception {
        String key = uniqueKey();
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicReference<Throwable> holderError = new AtomicReference<>();

        Thread holder = new Thread(() -> {
            LocalLock lock = new LocalLock(key, 0);
            try {
                lock.lock();
                acquired.countDown();
                release.await(10, TimeUnit.SECONDS);
            } catch (Throwable t) {
                holderError.set(t);
            } finally {
                lock.close();
            }
        }, "lock-holder");
        holder.start();

        assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

        LocalLock contender = new LocalLock(key, 0);
        assertThatThrownBy(contender::lock)
                .isInstanceOf(LockAcquireException.class)
                .hasMessageContaining("获取本地锁超时")
                .hasMessageContaining(key);

        release.countDown();
        holder.join(10_000);
        assertThat(holderError.get()).isNull();

        LocalLock afterRelease = new LocalLock(key, 0);
        assertThatCode(afterRelease::lock).doesNotThrowAnyException();
        afterRelease.close();
    }

    @Test
    void lock_withWaitMillis_timesOutWhenHeldByAnotherThread() throws Exception {
        String key = uniqueKey();
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            LocalLock lock = new LocalLock(key, 0);
            try {
                lock.lock();
                acquired.countDown();
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.close();
            }
        }, "lock-holder-timeout");
        holder.start();
        assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

        LocalLock contender = new LocalLock(key, 80);
        long begin = System.nanoTime();
        assertThatThrownBy(contender::lock).isInstanceOf(LockAcquireException.class);
        long elapsedMillis = (System.nanoTime() - begin) / 1_000_000L;

        assertThat(elapsedMillis).as("应在等待 waitMillis 后才失败").isGreaterThanOrEqualTo(70L);

        release.countDown();
        holder.join(10_000);
    }

    @Test
    void lock_withWaitMillis_succeedsOnceHolderReleases() throws Exception {
        String key = uniqueKey();
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch contenderWaiting = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            LocalLock lock = new LocalLock(key, 0);
            try {
                lock.lock();
                acquired.countDown();
                // 等竞争线程真正进入等待后再释放，确保「等待期内释放即成功」语义被验证
                contenderWaiting.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.close();
            }
        }, "lock-holder-release");
        holder.start();
        assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

        LocalLock contender = new LocalLock(key, 5_000);
        assertThatCode(() -> {
            contenderWaiting.countDown();
            contender.lock();
        }).as("等待期内持有者释放后应成功").doesNotThrowAnyException();
        contender.close();

        holder.join(10_000);
    }

    @Test
    void differentKeys_doNotBlockEachOther() throws Exception {
        String keyA = uniqueKey();
        String keyB = uniqueKey();
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread holder = new Thread(() -> {
            LocalLock lock = new LocalLock(keyA, 0);
            try {
                lock.lock();
                acquired.countDown();
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.close();
            }
        }, "lock-holder-other-key");
        holder.start();
        assertThat(acquired.await(5, TimeUnit.SECONDS)).isTrue();

        LocalLock other = new LocalLock(keyB, 0);
        assertThatCode(other::lock).doesNotThrowAnyException();
        other.close();

        release.countDown();
        holder.join(10_000);
    }

    @Test
    void lock_guaranteesMutualExclusionOfCriticalSection() throws Exception {
        String key = uniqueKey();
        int threads = 8;
        int iterations = 200;
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean overlapDetected = new AtomicBoolean(false);
        AtomicInteger inside = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < iterations; i++) {
                        LocalLock lock = new LocalLock(key, 10_000);
                        lock.lock();
                        try {
                            if (inside.incrementAndGet() != 1) {
                                overlapDetected.set(true);
                            }
                            counter.incrementAndGet();
                            inside.decrementAndGet();
                        } finally {
                            lock.close();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }, "mutex-" + t).start();
        }

        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        assertThat(overlapDetected).isFalse();
        assertThat(counter.get()).isEqualTo(threads * iterations);
    }

    // ---------- 异常类型 ----------

    @Test
    void lockAcquireException_isExtrasException() {
        LockAcquireException ex = new LockAcquireException("boom");

        assertThat(ex).isInstanceOf(ExtrasException.class).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void lockAcquireException_keepsCause() {
        InterruptedException cause = new InterruptedException("interrupted");
        LockAcquireException ex = new LockAcquireException("boom", cause);

        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getMessage()).isEqualTo("boom");
    }
}
