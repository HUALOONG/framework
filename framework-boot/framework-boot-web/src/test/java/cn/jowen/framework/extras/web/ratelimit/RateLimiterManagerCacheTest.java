package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.properties.RateLimitAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RateLimiterManager} 缓存键与工厂扩展点测试。
 *
 * <p>基础算法行为见 {@code RateLimiterManagerTest}；本类聚焦缓存键组成、
 * 自定义工厂注入、默认算法与并发下的实例唯一性。
 */
class RateLimiterManagerCacheTest {

    @Test
    void defaultConstructor_usesTokenBucket() {
        RateLimiterManager manager = new RateLimiterManager();

        assertThat(manager.get("api", 1, 60)).isInstanceOf(TokenBucketRateLimiter.class);
    }

    @ParameterizedTest
    @EnumSource(RateLimitAlgorithm.class)
    void factory_createsMatchingImplementationForEveryAlgorithm(RateLimitAlgorithm algorithm) {
        RateLimiterManager manager = new RateLimiterManager();
        RateLimiter limiter = manager.get("api", 1, 60, algorithm);

        Class<?> expected = switch (algorithm) {
            case FIXED_WINDOW -> FixedWindowRateLimiter.class;
            case SLIDING_WINDOW -> SlidingWindowRateLimiter.class;
            case LEAKY_BUCKET -> LeakyBucketRateLimiter.class;
            case TOKEN_BUCKET -> TokenBucketRateLimiter.class;
        };
        assertThat(limiter).isInstanceOf(expected);
    }

    @Test
    void cacheKey_includesPermits() {
        RateLimiterManager manager = new RateLimiterManager();

        assertThat(manager.get("api", 5, 60)).isNotSameAs(manager.get("api", 6, 60));
    }

    @Test
    void cacheKey_includesWindow() {
        RateLimiterManager manager = new RateLimiterManager();

        assertThat(manager.get("api", 5, 60)).isNotSameAs(manager.get("api", 5, 30));
    }

    @Test
    void cacheKey_includesAlgorithm() {
        RateLimiterManager manager = new RateLimiterManager();

        assertThat(manager.get("api", 5, 60, RateLimitAlgorithm.FIXED_WINDOW))
                .isNotSameAs(manager.get("api", 5, 60, RateLimitAlgorithm.SLIDING_WINDOW));
    }

    @Test
    void cacheKey_sameParametersReturnSameInstance() {
        RateLimiterManager manager = new RateLimiterManager();
        RateLimiter first = manager.get("api", 5, 60, RateLimitAlgorithm.LEAKY_BUCKET);

        assertThat(manager.get("api", 5, 60, RateLimitAlgorithm.LEAKY_BUCKET)).isSameAs(first);
    }

    @Test
    void explicitAlgorithm_matchingDefault_sharesCacheEntry() {
        RateLimiterManager manager = new RateLimiterManager(RateLimitAlgorithm.FIXED_WINDOW);

        assertThat(manager.get("api", 5, 60))
                .isSameAs(manager.get("api", 5, 60, RateLimitAlgorithm.FIXED_WINDOW));
    }

    @Test
    void customFactory_isUsedAndInvokedOncePerCacheKey() {
        AtomicInteger invocations = new AtomicInteger();
        RateLimiter stub = key -> true;
        RateLimiterManager manager = new RateLimiterManager(RateLimitAlgorithm.TOKEN_BUCKET,
                (permits, window, algorithm) -> {
                    invocations.incrementAndGet();
                    return stub;
                });

        assertThat(manager.get("api", 1, 60)).isSameAs(stub);
        assertThat(manager.get("api", 1, 60)).isSameAs(stub);
        assertThat(invocations.get()).as("同一缓存键只应创建一次").isEqualTo(1);
    }

    @Test
    void customFactory_receivesRequestedParameters() {
        RateLimiterManager manager = new RateLimiterManager(RateLimitAlgorithm.TOKEN_BUCKET,
                (permits, window, algorithm) -> {
                    assertThat(permits).isEqualTo(7);
                    assertThat(window).isEqualTo(15);
                    assertThat(algorithm).isEqualTo(RateLimitAlgorithm.LEAKY_BUCKET);
                    return key -> true;
                });

        assertThat(manager.get("api", 7, 15, RateLimitAlgorithm.LEAKY_BUCKET)).isNotNull();
    }

    @Test
    void get_returnsSingleSharedInstanceUnderConcurrency() throws Exception {
        RateLimiterManager manager = new RateLimiterManager();
        int threads = 16;
        Set<RateLimiter> seen = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 50; i++) {
                            seen.add(manager.get("concurrent", 10, 60));
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

        assertThat(seen).as("并发获取应收敛到同一实例").hasSize(1);
    }
}
