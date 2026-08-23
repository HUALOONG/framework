package cn.jowen.framework.extras.ratelimit.algorithm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SlidingWindowRateLimiter} 测试。
 */
class SlidingWindowRateLimiterTest {

    // 5 permits per 1 second window
    private final SlidingWindowRateLimiter limiter = new SlidingWindowRateLimiter(5, 1_000_000_000L);

    @Test
    void tryAcquire_initialState_success() {
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void tryAcquire_untilLimit_thenRejects() {
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
    void tryAcquire_moreThanPermits_fails() {
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
    void algorithm_returnsSlidingWindow() {
        assertThat(limiter.algorithm()).isEqualTo("sliding-window");
    }

    @Test
    void constructor_invalidPermits_throwsException() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(0, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_invalidPeriod_throwsException() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_negativePermits_throwsException() {
        assertThatThrownBy(() -> new SlidingWindowRateLimiter(-1, 1000L))
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
