package cn.jowen.framework.i18n.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageCodeUtils} 测试。
 */
class MessageCodeUtilsTest {

    @Test
    void join_singleSegment_returnsAsIs() {
        assertThat(MessageCodeUtils.join("user")).isEqualTo("user");
    }

    @Test
    void join_multipleSegments_withDots() {
        assertThat(MessageCodeUtils.join("user", "validate", "nameRequired"))
                .isEqualTo("user.validate.nameRequired");
    }

    @Test
    void join_emptyVarargs_returnsEmptyString() {
        assertThat(MessageCodeUtils.join()).isEmpty();
    }

    @Test
    void withPrefix_nullPrefix_returnsOriginalCode() {
        assertThat(MessageCodeUtils.withPrefix(null, "validate.name")).isEqualTo("validate.name");
    }

    @Test
    void withPrefix_emptyPrefix_returnsOriginalCode() {
        assertThat(MessageCodeUtils.withPrefix("", "validate.name")).isEqualTo("validate.name");
    }

    @Test
    void withPrefix_codeAlreadyHasPrefix_returnsOriginal() {
        assertThat(MessageCodeUtils.withPrefix("user", "user.validate.name"))
                .isEqualTo("user.validate.name");
    }

    @Test
    void withPrefix_addsPrefix() {
        assertThat(MessageCodeUtils.withPrefix("user", "validate.name"))
                .isEqualTo("user.validate.name");
    }

    @Test
    void normalize_underscoreToDot() {
        assertThat(MessageCodeUtils.normalize("user_validate_name"))
                .isEqualTo("user.validate.name");
    }

    @Test
    void normalize_hyphenToDot() {
        assertThat(MessageCodeUtils.normalize("user-validate-name"))
                .isEqualTo("user.validate.name");
    }

    @Test
    void normalize_trimWhitespace() {
        assertThat(MessageCodeUtils.normalize("  user.validate  ")).isEqualTo("user.validate");
    }

    @Test
    void normalize_nullReturnsNull() {
        assertThat(MessageCodeUtils.normalize(null)).isNull();
    }

    @Test
    void isInvalid_nullReturnsTrue() {
        assertThat(MessageCodeUtils.isInvalid(null)).isTrue();
    }

    @Test
    void isInvalid_emptyStringReturnsTrue() {
        assertThat(MessageCodeUtils.isInvalid("")).isTrue();
    }

    @Test
    void isInvalid_blankStringReturnsTrue() {
        assertThat(MessageCodeUtils.isInvalid("   ")).isTrue();
    }

    @Test
    void isInvalid_validCodeReturnsFalse() {
        assertThat(MessageCodeUtils.isInvalid("user.validate.name")).isFalse();
    }

    @Test
    void separatorIsDot() {
        assertThat(MessageCodeUtils.SEPARATOR).isEqualTo('.');
    }
}
