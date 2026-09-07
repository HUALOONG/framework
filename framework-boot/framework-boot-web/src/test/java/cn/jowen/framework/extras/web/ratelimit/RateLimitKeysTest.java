package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link RateLimitKeys} 命名规范验证。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
class RateLimitKeysTest {

    @Test
    void fixedWindowIncludesPrefixDimKeyAndWindowIndex() {
        RateLimitKeys keys = new RateLimitKeys("rt:");
        assertThat(keys.fixedWindow("login:1", 42L)).isEqualTo("rt:fw:login:1:42");
    }

    @Test
    void tokenBucketIncludesPrefixAndDimKey() {
        assertThat(new RateLimitKeys("rt:").tokenBucket("login:1")).isEqualTo("rt:tb:login:1");
    }

    @Test
    void defaultPrefixIsRatelimitColon() {
        assertThat(new RateLimitKeys().getPrefix()).isEqualTo("ratelimit:");
    }

    @Test
    void rejectsBlankPrefix() {
        assertThatThrownBy(() -> new RateLimitKeys(" "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("keyPrefix");
    }

    @Test
    void rejectsBlankDimKey() {
        RateLimitKeys keys = new RateLimitKeys();
        assertThatThrownBy(() -> keys.fixedWindow(" ", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dimKey");
        assertThatThrownBy(() -> keys.tokenBucket(" "))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("dimKey");
    }
}
