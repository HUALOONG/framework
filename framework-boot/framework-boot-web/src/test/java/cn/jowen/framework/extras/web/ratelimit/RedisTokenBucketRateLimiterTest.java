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
 * {@link RedisTokenBucketRateLimiter} 行为验证：通过 {@link FakeRedisCommandExecutor} 真实走通脚本链路。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
class RedisTokenBucketRateLimiterTest {

    @Test
    void allowsUpToCapacityThenRejects() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor().withClock(() -> 1_000_000L);
        RedisTokenBucketRateLimiter limiter =
                new RedisTokenBucketRateLimiter(exec, new RateLimitKeys(), 3, 60, true);

        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isTrue();
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void windowResetRefillsBucket() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RedisTokenBucketRateLimiter limiter =
                new RedisTokenBucketRateLimiter(exec, new RateLimitKeys(), 1, 60, true);

        // 预先植入一个已过窗口的桶状态（ts 早于当前 61s），验证窗口到期重置为满桶
        exec.put("ratelimit:tb:k", "0|" + (System.currentTimeMillis() - 61_000L), 0L);
        assertThat(limiter.tryAcquire("k")).isTrue();
        // 同窗口内再次请求应被拒绝
        assertThat(limiter.tryAcquire("k")).isFalse();
    }

    @Test
    void distinctKeysAreIsolated() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor().withClock(() -> 1_000_000L);
        RedisTokenBucketRateLimiter limiter =
                new RedisTokenBucketRateLimiter(exec, new RateLimitKeys(), 1, 60, true);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("a")).isFalse();
        assertThat(limiter.tryAcquire("b")).isTrue();
    }

    @Test
    void scriptCallCarriesExpectedArgs() {
        FakeRedisCommandExecutor exec = new FakeRedisCommandExecutor();
        RedisTokenBucketRateLimiter limiter =
                new RedisTokenBucketRateLimiter(exec, new RateLimitKeys("rt:"), 5, 30, true);

        limiter.tryAcquire("k");
        FakeRedisCommandExecutor.EvalCall call = exec.lastEval();
        assertThat(call).isNotNull();
        assertThat(call.script()).contains("TOKEN_BUCKET");
        assertThat(call.keys()).containsExactly("rt:tb:k");
        assertThat(call.args()).hasSize(4);
        assertThat(call.args().get(0)).isEqualTo("5");
        assertThat(call.args().get(1)).isEqualTo(Long.toString(30_000L));
        assertThat(call.args().get(3)).isEqualTo(Long.toString(3L * 30_000L));
        // ARGV[2]=nowMillis 由限流器以应用时间传入，只需为正且可解析
        assertThat(Long.parseLong(call.args().get(2))).isPositive();
    }

    @Test
    void failOpenAllowsWhenRedisThrows() {
        ThrowingExecutor exec = new ThrowingExecutor();
        RedisTokenBucketRateLimiter limiter =
                new RedisTokenBucketRateLimiter(exec, new RateLimitKeys(), 3, 60, true);

        assertThat(limiter.tryAcquire("k")).isTrue();
    }

    @Test
    void failClosedRejectsWhenRedisThrows() {
        ThrowingExecutor exec = new ThrowingExecutor();
        RedisTokenBucketRateLimiter limiter =
                new RedisTokenBucketRateLimiter(exec, new RateLimitKeys(), 3, 60, false);

        assertThatThrownBy(() -> limiter.tryAcquire("k"))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("backend unavailable");
    }

    @Test
    void rejectsWhenExecutorDoesNotSupportScript() {
        NoScriptExecutor exec = new NoScriptExecutor();

        assertThatThrownBy(() -> new RedisTokenBucketRateLimiter(exec, new RateLimitKeys(), 3, 60, true))
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
