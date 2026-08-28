package cn.jowen.framework.extras.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StringUtils} 测试。
 *
 * <p>覆盖：isBlank / isNotBlank 的空白判定（含全角与各类空白字符）、defaultIfBlank 兜底逻辑。
 */
class StringUtilsTest {

    // ---------- isBlank ----------

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", "\r", " \t\r\n "})
    void isBlank_returnsTrueForNullAndWhitespaceOnly(String value) {
        assertThat(StringUtils.isBlank(value)).isTrue();
        assertThat(StringUtils.isNotBlank(value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", " a ", "0", "中文", "\u3000x", "  .  "})
    void isBlank_returnsFalseWhenAnyNonWhitespacePresent(String value) {
        assertThat(StringUtils.isBlank(value)).isFalse();
        assertThat(StringUtils.isNotBlank(value)).isTrue();
    }

    @Test
    void isBlank_treatsIdeographicSpaceAsWhitespace() {
        // U+3000 全角空格：Character.isWhitespace 判定为 true
        assertThat(StringUtils.isBlank("\u3000")).isTrue();
    }

    @Test
    void isBlank_treatsNonBreakingSpaceAsNonWhitespace() {
        // U+00A0 不换行空格：Character.isWhitespace 判定为 false，属于既有行为
        assertThat(StringUtils.isBlank("\u00A0")).isFalse();
    }

    @Test
    void isBlank_acceptsAnyCharSequenceImplementation() {
        assertThat(StringUtils.isBlank(new StringBuilder("   "))).isTrue();
        assertThat(StringUtils.isBlank(new StringBuilder("  x"))).isFalse();
    }

    // ---------- defaultIfBlank ----------

    @Test
    void defaultIfBlank_returnsDefaultWhenValueBlank() {
        assertThat(StringUtils.defaultIfBlank(null, "fallback")).isEqualTo("fallback");
        assertThat(StringUtils.defaultIfBlank("", "fallback")).isEqualTo("fallback");
        assertThat(StringUtils.defaultIfBlank("  \t ", "fallback")).isEqualTo("fallback");
    }

    @Test
    void defaultIfBlank_returnsOriginalWhenValuePresent() {
        assertThat(StringUtils.defaultIfBlank("value", "fallback")).isEqualTo("value");
        assertThat(StringUtils.defaultIfBlank(" padded ", "fallback")).isEqualTo(" padded ");
    }

    @Test
    void defaultIfBlank_returnsNullWhenBothBlank() {
        assertThat(StringUtils.defaultIfBlank(null, null)).isNull();
        assertThat(StringUtils.defaultIfBlank("   ", null)).isNull();
    }
}
