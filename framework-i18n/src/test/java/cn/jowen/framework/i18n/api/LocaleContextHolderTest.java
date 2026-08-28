package cn.jowen.framework.i18n.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link LocaleContextHolder} 单元测试：验证读路径桥接（ContextCarrier 作用域值与 ThreadLocal 写入均可见）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class LocaleContextHolderTest {

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocale();
    }

    @Test
    void getLocale_defaultsWhenUnset() {
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void setLocale_visibleViaGetLocale() {
        LocaleContextHolder.setLocale(Locale.JAPANESE);
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.JAPANESE);
    }

    @Test
    void withLocale_visibleViaGetLocale() {
        Locale outer = LocaleContextHolder.getLocale();
        I18nContext.withLocale(Locale.FRENCH, () ->
                assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.FRENCH));
        // 作用域外恢复外层区域
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(outer);
    }

    @Test
    void resetLocale_clearsThreadLocal() {
        LocaleContextHolder.setLocale(Locale.KOREAN);
        LocaleContextHolder.resetLocale();
        assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
    }
}