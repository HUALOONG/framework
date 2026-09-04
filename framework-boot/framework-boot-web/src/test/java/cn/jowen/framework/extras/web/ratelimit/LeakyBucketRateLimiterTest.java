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
    void tryAcquire_recoversAsBucketLeaks() {
        // permits=2 / window=1s -> 每毫秒漏 0.002，约 500ms 漏掉一个额度
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(2, 1);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();

        // 等待桶漏出额度后重新放行（替代固定 Thread.sleep，容忍抖动）
        await().atMost(Duration.ofSeconds(3)).until(() -> limiter.tryAcquire("k"));
    }

    @Test
    void tryAcquire_stillRejectsWhenLeakTooSlow() {
        // permits=1 / window=100s -> 漏速极慢，短暂区间内仍不足以腾出额度
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, 100);

        assertThat(limiter.tryAcquire("k")).isTrue();
        // 验证在短暂区间内持续拒绝（替代固定 Thread.sleep）
        await().during(Duration.ofMillis(50)).atMost(Duration.ofSeconds(2))
                .until(() -> !limiter.tryAcquire("k"));
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
    void tryAcquire_nonPositiveWindowAllowsBurstWithoutThrottling() {
        // 非正窗口（windowSeconds<=0）语义为「不限流」：同一毫秒内连续请求也必须全部放行。
        // 修复前依赖 now>lastLeak 才漏空，同毫秒内已注水的桶不漏空 → 超出 capacity 后错误拒绝。
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, 0);

        for (int i = 0; i < 100; i++) {
            assertThat(limiter.tryAcquire("k")).as("非正窗口下不应限流").isTrue();
        }
    }

    @Test
    void tryAcquire_negativeWindowIsTreatedAsNonPositive() {
        LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(1, -5);

        for (int i = 0; i < 100; i++) {
            assertThat(limiter.tryAcquire("k")).isTrue();
        }
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
