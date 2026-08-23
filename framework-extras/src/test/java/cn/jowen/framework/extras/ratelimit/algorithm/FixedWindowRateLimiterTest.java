package cn.jowen.framework.extras.ratelimit.algorithm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FixedWindowRateLimiter} 测试。
 */
class FixedWindowRateLimiterTest {

    // 10 permits per 1 second window
    private final FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(10, 1_000_000_000L);

    @Test
    void tryAcquire_initialState_success() {
        assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    void tryAcquire_withinWindowLimit_success() {
        for (int i = 0; i < 10; i++) {
            assertThat(limiter.tryAcquire()).isTrue();
        }
    }

    @Test
    void tryAcquire_exceedWindowLimit_fails() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    void tryAcquire_multiplePermits_success() {
        assertThat(limiter.tryAcquire(5)).isTrue();
    }

    @Test
    void tryAcquire_moreThanPermits_fails() {
        assertThat(limiter.tryAcquire(100)).isFalse();
    }

    @Test
    void getAvailablePermits_initialFull() {
        assertThat(limiter.getAvailablePermits()).isEqualTo(10);
    }

    @Test
    void getAvailablePermits_afterConsuming() {
        limiter.tryAcquire(3);
        assertThat(limiter.getAvailablePermits()).isEqualTo(7);
    }

    @Test
    void algorithm_returnsFixedWindow() {
        assertThat(limiter.algorithm()).isEqualTo("fixed-window");
    }

    @Test
    void constructor_invalidPermits_throwsException() {
        assertThatThrownBy(() -> new FixedWindowRateLimiter(0, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_invalidPeriod_throwsException() {
        assertThatThrownBy(() -> new FixedWindowRateLimiter(10, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_negativePermits_throwsException() {
        assertThatThrownBy(() -> new FixedWindowRateLimiter(-1, 1000L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAvailablePermits_neverNegative() {
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire();
        }
        assertThat(limiter.getAvailablePermits()).isGreaterThanOrEqualTo(0);
    }
}
