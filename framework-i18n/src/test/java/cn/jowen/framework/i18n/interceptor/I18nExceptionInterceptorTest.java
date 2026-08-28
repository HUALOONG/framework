package cn.jowen.framework.i18n.interceptor;

import cn.jowen.framework.i18n.annotation.I18nException;
import cn.jowen.framework.i18n.api.MessageSource;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link I18nExceptionInterceptor} 测试。
 */
class I18nExceptionInterceptorTest {

    private final MessageSource messageSource = mock(MessageSource.class);
    private final I18nExceptionInterceptor interceptor = new I18nExceptionInterceptor(messageSource);

    @Test
    void resolve_nullThrowable_returnsNull() {
        assertThat(interceptor.resolve(null, Locale.CHINA)).isNull();
    }

    @Test
    void resolve_noAnnotation_returnsRawMessage() {
        RuntimeException ex = new RuntimeException("raw message");
        assertThat(interceptor.resolve(ex, Locale.CHINA)).isEqualTo("raw message");
    }

    @Test
    void resolve_annotated_usesMessageSource() {
        when(messageSource.getMessage(eq("error.business"), eq(Locale.CHINA), any()))
                .thenReturn("业务错误：%s");
        BusinessException ex = new BusinessException("detail");

        assertThat(interceptor.resolve(ex, Locale.CHINA)).isEqualTo("业务错误：%s");
    }

    @Test
    void resolve_messageNotFound_fallsBackToRaw() {
        when(messageSource.getMessage(eq("error.business"), eq(Locale.CHINA), any())).thenReturn(null);
        BusinessException ex = new BusinessException("detail");

        assertThat(interceptor.resolve(ex, Locale.CHINA)).isEqualTo("detail");
    }

    @Test
    void resolve_annotationOnSuperclass_found() {
        when(messageSource.getMessage(eq("error.base"), eq(Locale.CHINA), any())).thenReturn("基类异常");
        SubException ex = new SubException("boom");

        assertThat(interceptor.resolve(ex, Locale.CHINA)).isEqualTo("基类异常");
    }

    @Test
    void resolve_throwsException_messageNull() {
        when(messageSource.getMessage(eq("error.business"), eq(Locale.CHINA), any())).thenReturn("文案");
        BusinessException ex = new BusinessException(null);

        assertThat(interceptor.resolve(ex, Locale.CHINA)).isEqualTo("文案");
    }

    @I18nException("error.business")
    static class BusinessException extends RuntimeException {
        BusinessException(String message) {
            super(message);
        }
    }

    @I18nException("error.base")
    static class BaseException extends RuntimeException {
        BaseException(String message) {
            super(message);
        }
    }

    static class SubException extends BaseException {
        SubException(String message) {
            super(message);
        }
    }
}
