package cn.jowen.framework.core.desensitize;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link DesensitizeContext} 测试。
 */
class DesensitizeContextTest {

    @Test
    void mask_basic() {
        DesensitizeContext ctx = DesensitizeContext.of(3, 4);
        assertThat(ctx.mask("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void mask_fullMasking() {
        DesensitizeContext ctx = new DesensitizeContext(0, 0, "*", false);
        assertThat(ctx.mask("123456")).isEqualTo("******");
    }

    @Test
    void mask_nullReturnsNull() {
        assertThat(DesensitizeContext.DEFAULT.mask(null)).isNull();
    }

    @Test
    void mask_blankReturnsBlank() {
        assertThat(DesensitizeContext.DEFAULT.mask("  ")).isEqualTo("  ");
    }

    @Test
    void mask_skipReturnsRaw() {
        DesensitizeContext ctx = new DesensitizeContext(1, 1, "*", true);
        assertThat(ctx.mask("123456")).isEqualTo("123456");
    }

    @Test
    void mask_keepExceedsLength_returnsRaw() {
        DesensitizeContext ctx = DesensitizeContext.of(3, 4);
        assertThat(ctx.mask("abc")).isEqualTo("abc");
    }

    @Test
    void mask_customReplacement() {
        DesensitizeContext ctx = DesensitizeContext.of(1, 1, "#");
        assertThat(ctx.mask("123456")).isEqualTo("1####6");
    }

    @Test
    void mask_negativeStartKeep_treatedAsZero() {
        DesensitizeContext ctx = new DesensitizeContext(-1, -2, "*", false);
        assertThat(ctx.mask("123456")).isEqualTo("******");
    }

    @Test
    void mask_emptyReplacement_defaultsToAsterisk() {
        DesensitizeContext ctx = new DesensitizeContext(0, 0, "", false);
        assertThat(ctx.mask("12345")).isEqualTo("*****");
    }

    @Test
    void of_factory() {
        DesensitizeContext ctx = DesensitizeContext.of(2, 2);
        assertThat(ctx.mask("abcdef")).isEqualTo("ab**ef");
    }

    @Test
    void record_properties() {
        DesensitizeContext ctx = new DesensitizeContext(3, 4, "@", true);
        assertThat(ctx.startKeep()).isEqualTo(3);
        assertThat(ctx.endKeep()).isEqualTo(4);
        assertThat(ctx.replacement()).isEqualTo("@");
        assertThat(ctx.skip()).isTrue();
    }
}