package cn.jowen.framework.extras.ratelimit.algorithm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link LeakyBucketRateLimiter} 测试。
 */
class LeakyBucketRateLimiterTest {

    // capacity=5, drain 1 request per second
    private final LeakyBucketRateLimiter limiter = new LeakyBucketRateLimiter(5, 1_000_000_000L);

    @Test
    void tryAcquire_initialState_success() {
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void tryAcquire_untilFull_thenRejects() {
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire()).isTrue();
        }
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void tryAcquire_multiplePermits_successWhenSpace() {
        assertThat(limiter.tryAcquire(3)).isTrue();
    }

    @Test
    void tryAcquire_moreThanCapacity_fails() {
        assertThat(limiter.tryAcquire(10)).isFalse();
    }

    @Test
    void getAvailablePermits_initialFull() {
        assertThat(limiter.getAvailablePermits()).isEqualTo(5);
    }

    @Test
    void getAvailablePermits_afterConsuming() {
        limiter.tryAcquire(2);
        assertThat(limiter.getAvailablePermits()).isEqualTo(3);
    }

    @Test
    void algorithm_returnsLeakyBucket() {
        assertThat(limiter.algorithm()).isEqualTo("leaky-bucket");
    }

    @Test
    void constructor_invalidCapacity_throwsException() {
        assertThatThrownBy(() -> new LeakyBucketRateLimiter(0, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_negativeCapacity_throwsException() {
        assertThatThrownBy(() -> new LeakyBucketRateLimiter(-1, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_invalidDrainInterval_throwsException() {
        assertThatThrownBy(() -> new LeakyBucketRateLimiter(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAvailablePermits_neverNegative() {
        for (int i = 0; i < 5; i++) {
            limiter.tryAcquire();
        }
        assertThat(limiter.getAvailablePermits()).isEqualTo(0);
    }
}
