package cn.jowen.framework.extras.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SlidingWindowRateLimiter} 测试。
 *
 * <p>覆盖：窗口内配额、时间戳滑出后逐步放行、key 隔离、零配额、并发不超发。
 */
class SlidingWindowRateLimiterTest {

    @Test
    void tryAcquire_allowsUpToPermitsWithinWindow() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(3, 60);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_allowsAgainAfterOldHitsSlideOut() throws Exception {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();

        Thread.sleep(1_050L);

        assertThat(limiter.tryAcquire("k")).as("旧时间戳滑出窗口后应放行").isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_slidesPartially_notWholeWindowReset() throws Exception {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1);

        assertThat(limiter.tryAcquire("k")).isTrue();   // t0
        Thread.sleep(600L);
        assertThat(limiter.tryAcquire("k")).isTrue();   // t0+600
        assertThat(limiter.tryAcquire("k")).isFalse();

        // t0 的命中滑出，t0+600 的仍在窗口内 -> 仅腾出一个额度
        Thread.sleep(500L);
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).as("滑动窗口非整体重置").isFalse();
    }

    @Test
    void tryAcquire_isolatesDifferentKeys() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(1, 60);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void tryAcquire_rejectsAllWhenPermitsIsZero() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(0, 60);

        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_neverExceedsPermitsUnderConcurrency() throws Exception {
        int permits = 30;
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(permits, 600);
        int threads = 12;

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
}
