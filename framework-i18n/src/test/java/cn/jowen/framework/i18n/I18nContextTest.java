package cn.jowen.framework.i18n;

import cn.jowen.framework.i18n.api.I18nContext;
import java.util.Locale;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class I18nContextTest {

    @Test
    void withLocaleBindsScopedValue() {
        Locale zhCN = new Locale("zh", "CN");
        Locale inside = I18nContext.withLocale(zhCN, I18nContext::getCurrentLocale);
        assertThat(inside).isEqualTo(zhCN);
    }

    @Test
    void restoresOuterLocaleAfterScope() {
        I18nContext.withLocale(new Locale("zh", "CN"), () -> {
            assertThat(I18nContext.getCurrentLocale()).isEqualTo(new Locale("zh", "CN"));
            return null;
        });
        // 作用域外恢复默认
        assertThat(I18nContext.getCurrentLocale()).isEqualTo(Locale.getDefault());
    }

    @Test
    void defaultsToSystemLocaleWhenUnset() {
        assertThat(I18nContext.getCurrentLocaleOrDefault()).isEqualTo(Locale.getDefault());
    }

    @Test
    void nestedScopesOverridesThenRestores() {
        Locale zhCN = new Locale("zh", "CN");
        Locale en = Locale.ENGLISH;
        Locale result = I18nContext.withLocale(zhCN, () -> I18nContext.withLocale(en, I18nContext::getCurrentLocale));
        assertThat(result).isEqualTo(en);
        assertThat(I18nContext.getCurrentLocale()).isEqualTo(Locale.getDefault());
    }
}
