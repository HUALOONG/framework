package cn.jowen.framework.extras.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FixedWindowRateLimiter} 测试。
 *
 * <p>覆盖：窗口内配额、窗口切换清零、key 隔离、零配额、并发不超发。
 */
class FixedWindowRateLimiterTest {

    @Test
    void tryAcquire_allowsUpToPermitsWithinWindow() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(3, 60);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_resetsCountWhenWindowRolls() throws Exception {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(2, 1);

        // 对齐到窗口起始，避免刚好跨界导致的假失败
        alignToWindowStart(1_000L);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();

        // 等待进入下一个固定窗口
        Thread.sleep(1_050L);

        assertThat(limiter.tryAcquire("k")).as("窗口滚动后计数应清零").isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_isolatesDifferentKeys() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, 60);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void tryAcquire_rejectsAllWhenPermitsIsZero() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(0, 60);

        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_neverExceedsPermitsUnderConcurrency() throws Exception {
        int permits = 40;
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(permits, 600);
        int threads = 16;

        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 20; i++) {
                            if (limiter.tryAcquire("shared")) {
                                allowed.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(allowed.get()).isEqualTo(permits);
    }

    /** 睡到下一个窗口边界，使后续请求落在同一窗口内。 */
    private static void alignToWindowStart(long windowMillis) throws InterruptedException {
        long offset = System.currentTimeMillis() % windowMillis;
        long remaining = windowMillis - offset;
        if (remaining < windowMillis / 2) {
            Thread.sleep(remaining + 5L);
        }
    }
}
