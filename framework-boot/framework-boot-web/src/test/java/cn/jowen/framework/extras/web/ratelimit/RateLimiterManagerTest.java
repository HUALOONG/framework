package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.properties.RateLimitAlgorithm;
import cn.jowen.framework.extras.web.lock.FakeRedisCommandExecutor;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    @Test
    void managerUsesRedisFixedWindowWhenExecutorSupportsScript() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.FIXED_WINDOW, exec, new RateLimitKeys());

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.FIXED_WINDOW))
                .isInstanceOf(RedisFixedWindowRateLimiter.class);
    }

    @Test
    void managerUsesRedisTokenBucketWhenExecutorSupportsScript() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.TOKEN_BUCKET, exec, new RateLimitKeys());

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.TOKEN_BUCKET))
                .isInstanceOf(RedisTokenBucketRateLimiter.class);
    }

    @Test
    void managerFallsBackToLocalWhenNoExecutor() {
        RateLimiterManager manager = new RateLimiterManager();

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.FIXED_WINDOW))
                .isInstanceOf(FixedWindowRateLimiter.class);
    }

    @Test
    void managerFallsBackToLocalWhenExecutorLacksScript() {
        NoScriptExecutor exec = new NoScriptExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.FIXED_WINDOW, exec, new RateLimitKeys());

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.FIXED_WINDOW))
                .isInstanceOf(FixedWindowRateLimiter.class);
    }

    @Test
    void managerFallsBackToLocalSlidingWindowWhenNoExecutor() {
        RateLimiterManager manager = new RateLimiterManager();

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.SLIDING_WINDOW))
                .isInstanceOf(SlidingWindowRateLimiter.class);
    }

    @Test
    void managerFallsBackToLocalSlidingWindowWhenExecutorLacksScript() {
        NoScriptExecutor exec = new NoScriptExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.SLIDING_WINDOW, exec, new RateLimitKeys());

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.SLIDING_WINDOW))
                .isInstanceOf(SlidingWindowRateLimiter.class);
    }

    @Test
    void managerUsesRedisSlidingWindowWhenExecutorSupportsScript() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.SLIDING_WINDOW, exec, new RateLimitKeys());

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.SLIDING_WINDOW))
                .isInstanceOf(RedisSlidingWindowRateLimiter.class);
    }

    @Test
    void managerUsesRedisLeakyBucketWhenExecutorSupportsScript() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.LEAKY_BUCKET, exec, new RateLimitKeys());

        assertThat(manager.get("api", 2, 60, RateLimitAlgorithm.LEAKY_BUCKET))
                .isInstanceOf(RedisLeakyBucketRateLimiter.class);
    }

    @Test
    void redisSlidingWindowLimiterIsWiredThroughManagerAndEnforced() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.SLIDING_WINDOW, exec, new RateLimitKeys());
        RateLimiter limiter = manager.get("k", 1, 60, RateLimitAlgorithm.SLIDING_WINDOW);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void redisLeakyBucketLimiterIsWiredThroughManagerAndEnforced() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.LEAKY_BUCKET, exec, new RateLimitKeys());
        RateLimiter limiter = manager.get("k", 1, 60, RateLimitAlgorithm.LEAKY_BUCKET);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void fullConstructor_producesNonEmptyInstance() {
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.TOKEN_BUCKET, null, null, true);
        assertThat(manager.get("a", 1, 1)).isNotNull();
    }

    @Test
    void redisFixedWindowLimiterIsWiredThroughManagerAndEnforced() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RateLimiterManager manager =
                new RateLimiterManager(RateLimitAlgorithm.FIXED_WINDOW, exec, new RateLimitKeys());
        RateLimiter limiter = manager.get("k", 1, 60, RateLimitAlgorithm.FIXED_WINDOW);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    /** 不支持脚本的执行器（使用 default 方法），用于本地回落验证。 */
    @NullMarked
    static final class NoScriptExecutor implements RedisCommandExecutor {
        @Override
        public boolean setIfAbsent(String key, String value, long expireMillis) {
            return true;
        }

        @Override
        public @Nullable String get(String key) {
            return null;
        }

        @Override
        public void delete(String key) {
        }

        @Override
        public boolean deleteIfMatch(String key, String value) {
            return false;
        }
    }
}
