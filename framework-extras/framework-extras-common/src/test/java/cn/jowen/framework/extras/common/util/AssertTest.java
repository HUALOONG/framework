package cn.jowen.framework.extras.common.util;

import cn.jowen.framework.core.exception.ErrorCode;
import cn.jowen.framework.extras.common.exception.ErrorCodeEnum;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Assert} 测试。
 *
 * <p>覆盖：断言通过时静默返回、断言失败时抛出的异常类型 / 错误码 / 消息。
 */
class AssertTest {

    /** 自定义错误码，验证 Assert 能透传任意 ErrorCode 实现。 */
    private enum CustomCode implements ErrorCode {
        CUSTOM("C0001", "自定义错误");

        private final String code;
        private final String message;

        CustomCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        @Override
        public String code() {
            return code;
        }

        @Override
        public String message() {
            return message;
        }
    }

    // ---------- isTrue(boolean, String) ----------

    @Test
    void isTrue_withMessage_passesSilentlyWhenTrue() {
        assertThatCode(() -> Assert.isTrue(true, "不应抛出")).doesNotThrowAnyException();
    }

    @Test
    void isTrue_withMessage_throwsBusinessRejectedWhenFalse() {
        assertThatThrownBy(() -> Assert.isTrue(false, "业务不通过"))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("业务不通过")
                .extracting(e -> ((ExtrasException) e).getErrorCode())
                .isEqualTo(ErrorCodeEnum.BUSINESS_REJECTED);
    }

    // ---------- isTrue(boolean, ErrorCode) ----------

    @Test
    void isTrue_withErrorCode_passesSilentlyWhenTrue() {
        assertThatCode(() -> Assert.isTrue(true, ErrorCodeEnum.NOT_FOUND)).doesNotThrowAnyException();
    }

    @Test
    void isTrue_withErrorCode_usesErrorCodeMessageWhenFalse() {
        assertThatThrownBy(() -> Assert.isTrue(false, ErrorCodeEnum.NOT_FOUND))
                .isInstanceOf(ExtrasException.class)
                .hasMessage(ErrorCodeEnum.NOT_FOUND.message())
                .extracting(e -> ((ExtrasException) e).getErrorCode())
                .isEqualTo(ErrorCodeEnum.NOT_FOUND);
    }

    @Test
    void isTrue_withErrorCode_supportsCustomErrorCodeImplementation() {
        assertThatThrownBy(() -> Assert.isTrue(false, CustomCode.CUSTOM))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("自定义错误")
                .extracting(e -> ((ExtrasException) e).getErrorCode())
                .isEqualTo(CustomCode.CUSTOM);
    }

    // ---------- notNull ----------

    @Test
    void notNull_passesForNonNullValues() {
        assertThatCode(() -> Assert.notNull("value", "msg")).doesNotThrowAnyException();
        assertThatCode(() -> Assert.notNull(0, "msg")).doesNotThrowAnyException();
        assertThatCode(() -> Assert.notNull(false, "msg")).doesNotThrowAnyException();
        assertThatCode(() -> Assert.notNull("", "msg")).doesNotThrowAnyException();
    }

    @Test
    void notNull_throwsInvalidArgumentForNull() {
        assertThatThrownBy(() -> Assert.notNull(null, "对象不可为空"))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("对象不可为空")
                .extracting(e -> ((ExtrasException) e).getErrorCode())
                .isEqualTo(ErrorCodeEnum.INVALID_ARGUMENT);
    }

    // ---------- hasText ----------

    @ParameterizedTest
    @ValueSource(strings = {"a", " a ", "0", "中"})
    void hasText_passesWhenTextPresent(String text) {
        assertThatCode(() -> Assert.hasText(text, "msg")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "\t\n", "   "})
    void hasText_throwsInvalidArgumentForBlank(String text) {
        assertThatThrownBy(() -> Assert.hasText(text, "文本不可为空"))
                .isInstanceOf(ExtrasException.class)
                .hasMessage("文本不可为空")
                .extracting(e -> ((ExtrasException) e).getErrorCode())
                .isEqualTo(ErrorCodeEnum.INVALID_ARGUMENT);
    }

    // ---------- 异常可被统一捕获 ----------

    @Test
    void assertionFailures_areRuntimeExceptions() {
        RuntimeException ex = catchExtras(() -> Assert.notNull(null, "msg"));
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    private static RuntimeException catchExtras(Runnable runnable) {
        try {
            runnable.run();
        } catch (RuntimeException e) {
            return e;
        }
        throw new AssertionError("预期抛出异常但未抛出");
    }
}
