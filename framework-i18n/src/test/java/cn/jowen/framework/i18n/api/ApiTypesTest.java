package cn.jowen.framework.i18n.api;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MessageSourceResolvable}、{@link I18nContext}、{@link MessageNotFoundException}、
 * {@link FormatException}、{@link ResourceLoadException}、{@link I18nException} 及其
 * {@link I18nException.I18nErrorCode} 测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ApiTypesTest {

    // ===================== MessageSourceResolvable =====================

    @Test
    void resolvable_codeOnly() {
        MessageSourceResolvable resolvable = new MessageSourceResolvable("user.notFound");
        assertThat(resolvable.code()).isEqualTo("user.notFound");
        assertThat(resolvable.args()).isNull();
        assertThat(resolvable.defaultMessage()).isNull();
    }

    @Test
    void resolvable_codeAndDefaultMessage() {
        MessageSourceResolvable resolvable = new MessageSourceResolvable("user.notFound", "默认文案");
        assertThat(resolvable.code()).isEqualTo("user.notFound");
        assertThat(resolvable.args()).isNull();
        assertThat(resolvable.defaultMessage()).isEqualTo("默认文案");
    }

    @Test
    void resolvable_fullConstructor() {
        Object[] args = new Object[]{"tom", 3};
        MessageSourceResolvable resolvable = new MessageSourceResolvable("user.greeting", args, "hi {0}");
        assertThat(resolvable.code()).isEqualTo("user.greeting");
        assertThat(resolvable.args()).containsExactly("tom", 3);
        assertThat(resolvable.defaultMessage()).isEqualTo("hi {0}");
    }

    @Test
    void resolvable_nullCode_throws() {
        assertThatThrownBy(() -> new MessageSourceResolvable(null))
                .isInstanceOf(NullPointerException.class);
    }

    // ===================== I18nContext =====================

    @Test
    void context_withLocale_bindsLocaleForAction() {
        Locale result = I18nContext.withLocale(Locale.FRENCH, I18nContext::getCurrentLocale);
        assertThat(result).isEqualTo(Locale.FRENCH);
    }

    @Test
    void context_withLocale_propagatesToCurrentLocale() {
        Locale outside = I18nContext.withLocale(Locale.GERMAN, () -> {
            assertThat(I18nContext.getCurrentLocale()).isEqualTo(Locale.GERMAN);
            return I18nContext.getCurrentLocale();
        });
        assertThat(outside).isEqualTo(Locale.GERMAN);
    }

    @Test
    void context_setCurrentLocale_thenGet() {
        Locale resolved = I18nContext.withLocale(Locale.JAPANESE, () -> {
            I18nContext.setCurrentLocale(Locale.SIMPLIFIED_CHINESE);
            return I18nContext.getCurrentLocale();
        });
        assertThat(resolved).isEqualTo(Locale.SIMPLIFIED_CHINESE);
    }

    @Test
    void context_getCurrentLocaleOrDefault_neverNull() {
        assertThat(I18nContext.getCurrentLocaleOrDefault()).isNotNull();
    }

    @Test
    void context_withLocale_nullLocale_throws() {
        assertThatThrownBy(() -> I18nContext.withLocale(null, () -> "x"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void context_withLocale_nullAction_throws() {
        assertThatThrownBy(() -> I18nContext.withLocale(Locale.ENGLISH, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void context_setCurrentLocale_null_throws() {
        I18nContext.withLocale(Locale.ENGLISH, () -> {
            assertThatThrownBy(() -> I18nContext.setCurrentLocale(null))
                    .isInstanceOf(NullPointerException.class);
            return null;
        });
    }

    // ===================== I18nException / I18nErrorCode =====================

    @Test
    void errorCode_values_andMessages() {
        assertThat(I18nException.I18nErrorCode.values()).hasSize(3);
        assertThat(I18nException.I18nErrorCode.MESSAGE_NOT_FOUND.code()).isEqualTo("I18N_001");
        assertThat(I18nException.I18nErrorCode.MESSAGE_NOT_FOUND.message()).isEqualTo("消息未找到");
        assertThat(I18nException.I18nErrorCode.RESOURCE_LOAD_FAILED.code()).isEqualTo("I18N_002");
        assertThat(I18nException.I18nErrorCode.FORMAT_FAILED.code()).isEqualTo("I18N_003");
        assertThat(I18nException.I18nErrorCode.valueOf("FORMAT_FAILED"))
                .isEqualTo(I18nException.I18nErrorCode.FORMAT_FAILED);
    }

    @Test
    void i18nException_messageOnly() {
        I18nException ex = new I18nException("boom");
        assertThat(ex.getMessage()).isEqualTo("boom");
    }

    @Test
    void i18nException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        I18nException ex = new I18nException("boom", cause);
        assertThat(ex.getMessage()).isEqualTo("boom");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void i18nException_errorCodeOnly() {
        I18nException ex = new I18nException(I18nException.I18nErrorCode.MESSAGE_NOT_FOUND);
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.MESSAGE_NOT_FOUND);
    }

    @Test
    void i18nException_errorCodeAndCause() {
        Throwable cause = new RuntimeException("root");
        I18nException ex = new I18nException(I18nException.I18nErrorCode.FORMAT_FAILED, cause);
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.FORMAT_FAILED);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void i18nException_errorCodeAndMessage() {
        I18nException ex = new I18nException(I18nException.I18nErrorCode.RESOURCE_LOAD_FAILED, "自定义");
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.RESOURCE_LOAD_FAILED);
        assertThat(ex.getMessage()).isEqualTo("自定义");
    }

    @Test
    void i18nException_errorCodeMessageAndCause() {
        Throwable cause = new RuntimeException("root");
        I18nException ex = new I18nException(I18nException.I18nErrorCode.FORMAT_FAILED, "自定义", cause);
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.FORMAT_FAILED);
        assertThat(ex.getMessage()).isEqualTo("自定义");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    // ===================== 具体异常子类 =====================

    @Test
    void messageNotFoundException_message() {
        MessageNotFoundException ex = new MessageNotFoundException("code=foo");
        assertThat(ex).isInstanceOf(I18nException.class);
        assertThat(ex.getCode()).isEqualTo("code=foo");
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.MESSAGE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo("消息未找到: code=foo");
    }

    @Test
    void messageNotFoundException_nullCode() {
        MessageNotFoundException ex = new MessageNotFoundException(null);
        assertThat(ex.getCode()).isNull();
    }

    @Test
    void formatException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        FormatException ex = new FormatException("bad pattern", cause);
        assertThat(ex).isInstanceOf(I18nException.class);
        assertThat(ex.getMessage()).isEqualTo("bad pattern");
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.FORMAT_FAILED);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void formatException_messageOnly() {
        FormatException ex = new FormatException("bad pattern");
        assertThat(ex.getMessage()).isEqualTo("bad pattern");
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.FORMAT_FAILED);
    }

    @Test
    void resourceLoadException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        ResourceLoadException ex = new ResourceLoadException("load failed", cause);
        assertThat(ex).isInstanceOf(I18nException.class);
        assertThat(ex.getMessage()).isEqualTo("load failed");
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.RESOURCE_LOAD_FAILED);
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    void resourceLoadException_messageOnly() {
        ResourceLoadException ex = new ResourceLoadException("load failed");
        assertThat(ex.getMessage()).isEqualTo("load failed");
        assertThat(ex.getErrorCode()).isEqualTo(I18nException.I18nErrorCode.RESOURCE_LOAD_FAILED);
    }
}
