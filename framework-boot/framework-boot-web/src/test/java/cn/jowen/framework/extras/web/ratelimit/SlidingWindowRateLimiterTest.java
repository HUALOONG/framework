package cn.jowen.framework.extras.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

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
    void tryAcquire_allowsAgainAfterOldHitsSlideOut() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();

        // 等待旧时间戳滑出窗口后再次放行（替代固定 Thread.sleep，容忍抖动）
        await().atMost(Duration.ofSeconds(3)).until(() -> limiter.tryAcquire("k"));

        assertThat(limiter.tryAcquire("k")).as("旧时间戳滑出窗口后应放行").isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_slidesPartially_notWholeWindowReset() {
        SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(2, 1);

        long t0 = System.currentTimeMillis();
        assertThat(limiter.tryAcquire("k")).isTrue();   // 第一笔命中

        // 等约 600ms 让第二笔命中落在 t0 之后，制造「部分滑出」窗口
        await().atMost(Duration.ofSeconds(2)).until(() -> System.currentTimeMillis() - t0 >= 600L);
        assertThat(limiter.tryAcquire("k")).isTrue();   // 第二笔命中，窗口满载
        assertThat(limiter.tryAcquire("k")).isFalse();

        // 最早一笔命中滑出窗口、但稍晚一笔仍在窗口内 -> 仅腾出一个额度（非整体重置）
        await().atMost(Duration.ofSeconds(3)).until(() -> {
            boolean first = limiter.tryAcquire("k");
            boolean second = limiter.tryAcquire("k");
            return first && !second;
        });

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
