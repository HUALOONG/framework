package cn.jowen.framework.core.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link StringUtils} 测试。
 */
class StringUtilsTest {

    @Test
    void isEmpty_nullIsTrue() {
        assertThat(StringUtils.isEmpty(null)).isTrue();
    }

    @Test
    void isEmpty_emptyStringIsTrue() {
        assertThat(StringUtils.isEmpty("")).isTrue();
    }

    @Test
    void isEmpty_blankStringIsTrue() {
        assertThat(StringUtils.isEmpty("   ")).isTrue();
    }

    @Test
    void isEmpty_normalStringIsFalse() {
        assertThat(StringUtils.isEmpty("hello")).isFalse();
    }

    @Test
    void isNotEmpty_inverseOfIsEmpty() {
        assertThat(StringUtils.isNotEmpty(null)).isFalse();
        assertThat(StringUtils.isNotEmpty("")).isFalse();
        assertThat(StringUtils.isNotEmpty("  ")).isFalse();
        assertThat(StringUtils.isNotEmpty("abc")).isTrue();
    }

    @Test
    void equals_bothNull_returnsTrue() {
        assertThat(StringUtils.equals(null, null)).isTrue();
    }

    @Test
    void equals_oneNull_returnsFalse() {
        assertThat(StringUtils.equals(null, "a")).isFalse();
        assertThat(StringUtils.equals("a", null)).isFalse();
    }

    @Test
    void equals_sameContent_returnsTrue() {
        assertThat(StringUtils.equals("hi", "hi")).isTrue();
    }

    @Test
    void equals_differentContent_returnsFalse() {
        assertThat(StringUtils.equals("hi", "hello")).isFalse();
    }
}