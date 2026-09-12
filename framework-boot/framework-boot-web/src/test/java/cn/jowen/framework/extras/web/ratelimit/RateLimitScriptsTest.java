package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link RateLimitScripts} 常量完整性验证：确保脚本含可识别标记且语法关键调用存在，
 * 以便 {@link FakeRedisCommandExecutor} 能按片段包含正确分派。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
class RateLimitScriptsTest {

    @Test
    void fixedWindowConstantReferencesMarkerAndIncR() {
        assertThat(RateLimitScripts.FIXED_WINDOW)
                .contains("FIXED_WINDOW")
                .contains("INCR")
                .contains("PEXPIRE");
    }

    @Test
    void tokenBucketConstantReferencesMarkerAndHash() {
        assertThat(RateLimitScripts.TOKEN_BUCKET)
                .contains("TOKEN_BUCKET")
                .contains("HMGET")
                .contains("HSET");
    }

    @Test
    void slidingWindowConstantReferencesMarkerAndZsetOps() {
        assertThat(RateLimitScripts.SLIDING_WINDOW)
                .contains("SLIDING_WINDOW")
                .contains("ZREMRANGEBYSCORE")
                .contains("ZCARD")
                .contains("ZADD");
    }

    @Test
    void leakyBucketConstantReferencesMarkerAndHash() {
        assertThat(RateLimitScripts.LEAKY_BUCKET)
                .contains("LEAKY_BUCKET")
                .contains("HMGET")
                .contains("HSET");
    }
}
