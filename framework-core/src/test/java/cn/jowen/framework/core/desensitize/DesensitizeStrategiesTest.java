package cn.jowen.framework.core.desensitize;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * {@link DesensitizeStrategies} 测试。
 */
class DesensitizeStrategiesTest {

    @Test
    void phone_mask() {
        assertThat(DesensitizeStrategies.PHONE.mask("13812345678")).isEqualTo("138****5678");
    }

    @Test
    void phone_matches_valid() {
        assertThat(DesensitizeStrategies.PHONE.matches("13812345678")).isTrue();
    }

    @Test
    void phone_matches_invalid_returnsFalse() {
        assertThat(DesensitizeStrategies.PHONE.matches("12812345678")).isFalse();
        assertThat(DesensitizeStrategies.PHONE.matches("")).isFalse();
        assertThat(DesensitizeStrategies.PHONE.matches(null)).isFalse();
    }

    @Test
    void phone_mask_invalidFormat_returnsRaw() {
        assertThat(DesensitizeStrategies.PHONE.mask("12812345678")).isEqualTo("12812345678");
    }

    @Test
    void idCard_mask() {
        assertThat(DesensitizeStrategies.ID_CARD.mask("11010519491231002X")).isEqualTo("110***********002X");
    }

    @Test
    void bankCard_mask() {
        assertThat(DesensitizeStrategies.BANK_CARD.mask("6217000010012345678")).isEqualTo("6217***********5678");
    }

    @Test
    void email_mask() {
        assertThat(DesensitizeStrategies.EMAIL.mask("test@example.com")).isEqualTo("t***@example.com");
    }

    @Test
    void name_mask() {
        assertThat(DesensitizeStrategies.NAME.mask("张小明")).isEqualTo("张**");
    }

    @Test
    void address_mask() {
        assertThat(DesensitizeStrategies.ADDRESS.mask("abcdefg")).isEqualTo("abc****");
    }

    @Test
    void password_mask_full() {
        assertThat(DesensitizeStrategies.PASSWORD.mask("secret123")).isEqualTo("*********");
    }

    @Test
    void fixedPhone_mask() {
        assertThat(DesensitizeStrategies.FIXED_PHONE.mask("010-12345678")).isEqualTo("010*****5678");
    }

    @Test
    void licensePlate_mask() {
        assertThat(DesensitizeStrategies.LICENSE_PLATE.mask("京A12345")).isEqualTo("京A1**45");
    }

    @Test
    void custom_withContext() {
        assertThat(DesensitizeStrategies.CUSTOM.mask("abc123def", DesensitizeContext.of(1, 1))).isEqualTo("a*******f");
    }

    @Test
    void matches_nullOrBlank_returnsFalse() {
        assertThat(DesensitizeStrategies.ADDRESS.matches(null)).isFalse();
        assertThat(DesensitizeStrategies.ADDRESS.matches("  ")).isFalse();
    }

    @Test
    void defaultStartKeep() {
        assertThat(DesensitizeStrategies.PHONE.defaultStartKeep()).isEqualTo(3);
    }

    @Test
    void defaultEndKeep() {
        assertThat(DesensitizeStrategies.PHONE.defaultEndKeep()).isEqualTo(4);
    }
}