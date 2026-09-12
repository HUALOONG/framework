package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.web.lock.FakeRedisCommandExecutor;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RedisSlidingWindowRateLimiter} 行为验证：通过 {@link FakeRedisCommandExecutor} 真实走通脚本链路。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
class RedisSlidingWindowRateLimiterTest {

    @Test
    void allowsUpToPermitsThenRejects() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RedisSlidingWindowRateLimiter limiter =
                new RedisSlidingWindowRateLimiter(exec, new RateLimitKeys(), 3, 60, true);

        assertThat(limiter.tryAcquire("login:1")).isTrue();
        assertThat(limiter.tryAcquire("login:1")).isTrue();
        assertThat(limiter.tryAcquire("login:1")).isTrue();
        assertThat(limiter.tryAcquire("login:1")).isFalse();
    }

    @Test
    void distinctKeysAreIsolated() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RedisSlidingWindowRateLimiter limiter =
                new RedisSlidingWindowRateLimiter(exec, new RateLimitKeys(), 1, 60, true);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void scriptCallCarriesExpectedArgs() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RedisSlidingWindowRateLimiter limiter =
                new RedisSlidingWindowRateLimiter(exec, new RateLimitKeys("rt:"), 5, 30, true);

        limiter.tryAcquire("k");
        FakeRedisCommandExecutor.EvalCall call = exec.lastEval();
        assertThat(call).isNotNull();
        assertThat(call.script()).contains("SLIDING_WINDOW");
        assertThat(call.keys()).hasSize(1);
        assertThat(call.keys().get(0)).startsWith("rt:sw:k");
        assertThat(call.args()).hasSize(4);
        // ARGV[0]=nowSeconds：当前时间（毫秒）/1000，取整为秒级浮点
        double nowSec = Double.parseDouble(call.args().get(0));
        assertThat(nowSec).isPositive();
        // ARGV[1]=windowMillis / 1000.0
        double windowSec = Double.parseDouble(call.args().get(1));
        assertThat(windowSec).isEqualTo(30.0);
        // ARGV[2]=permits
        assertThat(call.args().get(2)).isEqualTo("5");
        // ARGV[3]=ttl = 2 * windowMillis
        assertThat(call.args().get(3)).isEqualTo(Long.toString(2L * 30_000L));
    }

    @Test
    void failOpenAllowsWhenRedisThrows() {
        ThrowingExecutor exec = new ThrowingExecutor();
        RedisSlidingWindowRateLimiter limiter =
                new RedisSlidingWindowRateLimiter(exec, new RateLimitKeys(), 3, 60, true);

        assertThat(limiter.tryAcquire("k")).isTrue();
    }

    @Test
    void failClosedRejectsWhenRedisThrows() {
        ThrowingExecutor exec = new ThrowingExecutor();
        RedisSlidingWindowRateLimiter limiter =
                new RedisSlidingWindowRateLimiter(exec, new RateLimitKeys(), 3, 60, false);

        assertThatThrownBy(() -> limiter.tryAcquire("k"))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("backend unavailable");
    }

    @Test
    void rejectsWhenExecutorDoesNotSupportScript() {
        NoScriptExecutor exec = new NoScriptExecutor();

        assertThatThrownBy(() -> new RedisSlidingWindowRateLimiter(exec, new RateLimitKeys(), 3, 60, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must support script");
    }

    /** 始终抛出异常的脚本执行器（支持脚本），用于 fail-open / fail-closed 验证。 */
    @NullMarked
    static final class ThrowingExecutor implements RedisCommandExecutor {
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

        @Override
        public boolean supportsScript() {
            return true;
        }

        @Override
        public @Nullable Object eval(String script, List<String> keys, List<String> args) {
            throw new RuntimeException("boom");
        }
    }

    /** 不支持脚本的执行器（使用 default 方法），用于构造期校验验证。 */
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
