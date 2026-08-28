package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.properties.RateLimitAlgorithm;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 四种限流算法的基础行为验证：均在超出配额后拒绝。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class RateLimiterManagerTest {

    @Test
    void tokenBucketAllowsUpToPermits() {
        RateLimiter limiter = new TokenBucketRateLimiter(3, 60);
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void fixedWindowAllowsUpToPermits() {
        RateLimiter limiter = new FixedWindowRateLimiter(2, 60);
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void slidingWindowAllowsUpToPermits() {
        RateLimiter limiter = new SlidingWindowRateLimiter(2, 60);
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void leakyBucketAllowsUpToPermits() {
        RateLimiter limiter = new LeakyBucketRateLimiter(2, 60);
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void managerCachesInstancePerDimension() {
        RateLimiterManager manager = new RateLimiterManager();
        RateLimiter a = manager.get("api", 5, 60);
        assertThat(manager.get("api", 5, 60)).isSameAs(a);
        assertThat(manager.get("other", 5, 60)).isNotSameAs(a);
    }

    @Test
    void managerRespectsRequestedAlgorithm() {
        RateLimiterManager manager = new RateLimiterManager(RateLimitAlgorithm.FIXED_WINDOW);
        assertThat(manager.get("a", 1, 60, RateLimitAlgorithm.SLIDING_WINDOW))
                .isInstanceOf(SlidingWindowRateLimiter.class);
        assertThat(manager.get("b", 1, 60)).isInstanceOf(FixedWindowRateLimiter.class);
    }

    @Test
    void distinctDimensionsAreIsolated() {
        RateLimiterManager manager = new RateLimiterManager();
        RateLimiter a = manager.get("user:1", 1, 60);
        RateLimiter b = manager.get("user:2", 1, 60);

        assertThat(a.tryAcquire("x")).isTrue();
        assertThat(a.tryAcquire("x")).isFalse();
        assertThat(b.tryAcquire("x")).isTrue();
    }
}
