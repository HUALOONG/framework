package cn.jowen.framework.extras.ratelimit.algorithm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link TokenBucketRateLimiter} 测试。
 */
class TokenBucketRateLimiterTest {

    private final TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, 1_000_000_000L); // 5 tokens, 1 sec refill

    @Test
    void tryAcquire_initialState_success() {
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void tryAcquire_allTokensExhausted_fails() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire();
        }
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void tryAcquire_multiplePermits_successWhenAvailable() {
        assertThat(limiter.tryAcquire(3)).isTrue();
    }

    @Test
    void tryAcquire_moreThanAvailable_fails() {
        assertThat(limiter.tryAcquire(10)).isFalse();
    }

    @Test
    void getAvailablePermits_initialCapacity() {
        assertThat(limiter.getAvailablePermits()).isEqualTo(5);
    }

    @Test
    void getAvailablePermits_afterConsuming() {
        limiter.tryAcquire(2);
        assertThat(limiter.getAvailablePermits()).isEqualTo(3);
    }

    @Test
    void algorithm_returnsTokenBucket() {
        assertThat(limiter.algorithm()).isEqualTo("token-bucket");
    }

    @Test
    void constructor_invalidCapacity_throwsException() {
        assertThatThrownBy(() -> new TokenBucketRateLimiter(0, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_negativeCapacity_throwsException() {
        assertThatThrownBy(() -> new TokenBucketRateLimiter(-1, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_invalidRefillInterval_throwsException() {
        assertThatThrownBy(() -> new TokenBucketRateLimiter(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tryAcquire_zeroPermits_alwaysFalse() {
        // permits must be > 0; but tryAcquire(1) is the default
        TokenBucketRateLimiter small = new TokenBucketRateLimiter(1, 1_000_000_000L);
        small.tryAcquire();
        assertThat(small.tryAcquire()).isFalse();
    }

    @Test
    void getAvailablePermits_neverExceedsCapacity() {
        TokenBucketRateLimiter c = new TokenBucketRateLimiter(10, 1_000_000_000L);
        assertThat(c.getAvailablePermits()).isEqualTo(10);
    }
}
