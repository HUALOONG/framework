package cn.jowen.framework.extras.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link TokenBucketRateLimiter} 测试。
 *
 * <p>覆盖：配额消耗、窗口到期后补满、key 维度隔离、零/负配额、并发下不超发。
 */
class TokenBucketRateLimiterTest {

    @Test
    void tryAcquire_consumesExactlyPermitsPerWindow() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 60);

        for (int i = 1; i <= 5; i++) {
            assertThat(limiter.tryAcquire("k")).as("第 %d 次应放行", i).isTrue();
        }
        assertThat(limiter.tryAcquire("k")).isFalse();
        assertThat(limiter.tryAcquire("k")).as("耗尽后持续拒绝").isFalse();
    }

    @Test
    void tryAcquire_refillsAfterWindowElapsed() throws Exception {
        // windowSeconds 参数为 int 秒，用 1 秒窗口验证补充语义
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(2, 1);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();

        Thread.sleep(1_050L);

        assertThat(limiter.tryAcquire("k")).as("窗口过期后令牌应补满").isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_isolatesDifferentKeys() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 60);

        assertThat(limiter.tryAcquire("user:1")).isTrue();
        assertThat(limiter.tryAcquire("user:1")).isFalse();
        assertThat(limiter.tryAcquire("user:2")).as("不同 key 各自独立计数").isTrue();
        assertThat(limiter.tryAcquire("user:3")).isTrue();
    }

    @Test
    void tryAcquire_rejectsEverythingWhenPermitsIsZero() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(0, 60);

        assertThat(limiter.tryAcquire("k")).isFalse();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_rejectsEverythingWhenPermitsIsNegative() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(-1, 60);

        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_neverExceedsPermitsUnderConcurrency() throws Exception {
        int permits = 50;
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(permits, 600);
        int threads = 16;
        int attemptsPerThread = 30;

        AtomicInteger allowed = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < attemptsPerThread; i++) {
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

        assertThat(allowed.get()).as("并发下放行数不得超过配额").isEqualTo(permits);
    }

    @Test
    void tryAcquire_treatsZeroWindowAsImmediateRefill() {
        // windowSeconds = 0 -> windowMillis = 0，每次调用都视为新窗口
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(1, 0);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).as("零窗口应每次都重置令牌").isTrue();
    }
}
