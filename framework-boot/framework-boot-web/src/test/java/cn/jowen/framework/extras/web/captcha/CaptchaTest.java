package cn.jowen.framework.extras.web.captcha;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link Captcha} 记录类行为测试。
 *
 * <p>覆盖：expired 的时间判定与边界、expireAtInstant 转换、可空字段承载、
 * 以及 record 对 {@code byte[]} 采用引用相等的既有语义。
 */
class CaptchaTest {

    private static Captcha withExpireAt(long expireAt) {
        return new Captcha("id-1", "1234", "题面", null, expireAt);
    }

    // ---------- expired ----------

    @Test
    void expired_isFalseForFutureExpiry() {
        assertThat(withExpireAt(System.currentTimeMillis() + 60_000L).expired()).isFalse();
    }

    @Test
    void expired_isTrueForPastExpiry() {
        assertThat(withExpireAt(System.currentTimeMillis() - 1L).expired()).isTrue();
    }

    @Test
    void expired_isFalseExactlyAtBoundary() {
        // 实现使用严格大于比较：now > expireAt 才算过期
        long farFuture = Long.MAX_VALUE;
        assertThat(withExpireAt(farFuture).expired()).isFalse();
    }

    @Test
    void expired_isTrueForZeroExpiry() {
        assertThat(withExpireAt(0L).expired()).isTrue();
    }

    @Test
    void expired_flipsAsTimePasses() throws Exception {
        Captcha captcha = withExpireAt(System.currentTimeMillis() + 30L);

        assertThat(captcha.expired()).isFalse();
        Thread.sleep(60L);
        assertThat(captcha.expired()).isTrue();
    }

    // ---------- expireAtInstant ----------

    @Test
    void expireAtInstant_matchesEpochMilli() {
        long expireAt = 1_700_000_000_000L;

        assertThat(withExpireAt(expireAt).expireAtInstant())
                .isEqualTo(Instant.ofEpochMilli(expireAt));
    }

    @Test
    void expireAtInstant_isConsistentWithExpiredFlag() {
        Captcha captcha = withExpireAt(System.currentTimeMillis() + 60_000L);

        assertThat(captcha.expireAtInstant()).isAfter(Instant.now());
        assertThat(captcha.expired()).isFalse();
    }

    // ---------- 字段承载 ----------

    @Test
    void accessors_returnConstructorValues() {
        byte[] image = {1, 2, 3};
        Captcha captcha = new Captcha("id-9", "code-9", "text-9", image, 42L);

        assertThat(captcha.id()).isEqualTo("id-9");
        assertThat(captcha.code()).isEqualTo("code-9");
        assertThat(captcha.text()).isEqualTo("text-9");
        assertThat(captcha.image()).isSameAs(image);
        assertThat(captcha.expireAt()).isEqualTo(42L);
    }

    @Test
    void nullableFields_acceptNull() {
        Captcha captcha = new Captcha("id", "code", null, null, 1L);

        assertThat(captcha.text()).isNull();
        assertThat(captcha.image()).isNull();
    }

    /** 现状记录：record 自动生成的 equals 对数组按引用比较，调用方不应依赖值相等。 */
    @Test
    void equals_usesReferenceComparisonForImageArray() {
        byte[] shared = {1, 2, 3};

        assertThat(new Captcha("id", "code", "t", shared, 1L))
                .isEqualTo(new Captcha("id", "code", "t", shared, 1L));
        assertThat(new Captcha("id", "code", "t", new byte[]{1, 2, 3}, 1L))
                .isNotEqualTo(new Captcha("id", "code", "t", new byte[]{1, 2, 3}, 1L));
    }

    @Test
    void equals_comparesScalarFields() {
        assertThat(new Captcha("id", "code", "t", null, 1L))
                .isEqualTo(new Captcha("id", "code", "t", null, 1L))
                .isNotEqualTo(new Captcha("other", "code", "t", null, 1L))
                .isNotEqualTo(new Captcha("id", "other", "t", null, 1L))
                .isNotEqualTo(new Captcha("id", "code", "t", null, 2L));
    }
}
