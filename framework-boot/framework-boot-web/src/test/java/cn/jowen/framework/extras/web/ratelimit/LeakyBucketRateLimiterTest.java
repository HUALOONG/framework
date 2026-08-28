package cn.jowen.framework.extras.web.ratelimit;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LeakyBucketRateLimiter} 测试。
 *
 * <p>覆盖：桶容量上限、按时间恒速漏水后恢复放行、key 隔离、零/非法窗口的降级行为、并发不超发。
 */
class LeakyBucketRateLimiterTest {

    @Test
    void tryAcquire_allowsUpToCapacity() {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(3, 60);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_recoversAsBucketLeaks() throws Exception {
        // permits=2 / window=1s -> 每毫秒漏 0.002，约 500ms 漏掉一个额度
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(2, 1);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();

        Thread.sleep(700L);

        assertThat(limiter.tryAcquire("k")).as("漏水后应重新放行").isTrue();
    }

    @Test
    void tryAcquire_stillRejectsWhenLeakTooSlow() throws Exception {
        // permits=1 / window=100s -> 漏速极慢，短暂等待不足以腾出额度
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, 100);

        assertThat(limiter.tryAcquire("k")).isTrue();
        Thread.sleep(50L);
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_isolatesDifferentKeys() {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, 600);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void tryAcquire_rejectsAllWhenCapacityIsZero() {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(0, 60);

        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void tryAcquire_nonPositiveWindowDrainsBucketImmediately() throws Exception {
        // windowSeconds <= 0 时漏速取 Double.MAX_VALUE，等价于"不限流"
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, 0);

        assertThat(limiter.tryAcquire("k")).isTrue();
        Thread.sleep(5L);
        assertThat(limiter.tryAcquire("k")).as("非正窗口下桶应立即漏空").isTrue();
        Thread.sleep(5L);
        assertThat(limiter.tryAcquire("k")).isTrue();
    }

    @Test
    void tryAcquire_negativeWindowIsTreatedAsNonPositive() throws Exception {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, -5);

        assertThat(limiter.tryAcquire("k")).isTrue();
        Thread.sleep(5L);
        assertThat(limiter.tryAcquire("k")).isTrue();
    }

    @Test
    void tryAcquire_neverExceedsCapacityUnderConcurrency() throws Exception {
        int capacity = 30;
        // 极慢漏速，确保并发窗口内几乎不漏水
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(capacity, 100_000);
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

        assertThat(allowed.get()).isEqualTo(capacity);
    }
}
