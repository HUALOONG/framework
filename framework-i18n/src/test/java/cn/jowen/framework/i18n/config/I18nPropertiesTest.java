package cn.jowen.framework.i18n.config;

import cn.jowen.framework.i18n.reload.ReloadStrategy;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link I18nProperties} 测试。
 *
 * @author 王飞
 * @since 2026-08-24
 */
class I18nPropertiesTest {

    private final I18nProperties properties = new I18nProperties();

    @Test
    void defaultValues() {
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getSource()).isEqualTo(SourceType.PROPERTIES);
        assertThat(properties.getBasename()).isEqualTo("messages");
        assertThat(properties.getResolver()).isEqualTo(ResolverType.PARAMETER);
        assertThat(properties.getFormatter()).isEqualTo(FormatterType.JAVA_TEXT);
        assertThat(properties.getDefaultLocale()).isEmpty();
        assertThat(properties.getReloadIntervalSeconds()).isEqualTo(0L);
        assertThat(properties.getSupportedLocales()).isEmpty();
        assertThat(properties.getCompositeOrder()).isEmpty();
        assertThat(properties.getCacheSeconds()).isEqualTo(3600L);
        assertThat(properties.getReloadStrategy()).isEqualTo(ReloadStrategy.MANUAL);
        assertThat(properties.getDatabase().getTableName()).isEqualTo("i18n_message");
        assertThat(properties.getDatabase().getLocaleColumn()).isEqualTo("locale");
        assertThat(properties.getDatabase().getCodeColumn()).isEqualTo("code");
        assertThat(properties.getDatabase().getMessageColumn()).isEqualTo("message");
    }

    @Test
    void setEnabled_getEnabled() {
        properties.setEnabled(false);
        assertThat(properties.isEnabled()).isFalse();
        properties.setEnabled(true);
        assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    void setSource_getSource() {
        properties.setSource(SourceType.DATABASE);
        assertThat(properties.getSource()).isEqualTo(SourceType.DATABASE);
        properties.setSource(SourceType.PROPERTIES);
        assertThat(properties.getSource()).isEqualTo(SourceType.PROPERTIES);
    }

    @Test
    void setBasename_getBasename() {
        properties.setBasename("i18n.messages");
        assertThat(properties.getBasename()).isEqualTo("i18n.messages");
    }

    @Test
    void setResolver_getResolver() {
        properties.setResolver(ResolverType.FIXED);
        assertThat(properties.getResolver()).isEqualTo(ResolverType.FIXED);
        properties.setResolver(ResolverType.ACCEPT_HEADER);
        assertThat(properties.getResolver()).isEqualTo(ResolverType.ACCEPT_HEADER);
    }

    @Test
    void setFormatter_getFormatter() {
        properties.setFormatter(FormatterType.NAMED_PARAMETER);
        assertThat(properties.getFormatter()).isEqualTo(FormatterType.NAMED_PARAMETER);
        properties.setFormatter(FormatterType.ICU);
        assertThat(properties.getFormatter()).isEqualTo(FormatterType.ICU);
    }

    @Test
    void setDefaultLocale_getDefaultLocale() {
        properties.setDefaultLocale("zh_CN");
        assertThat(properties.getDefaultLocale()).isEqualTo("zh_CN");
    }

    @Test
    void setReloadIntervalSeconds_getReloadIntervalSeconds() {
        properties.setReloadIntervalSeconds(30);
        assertThat(properties.getReloadIntervalSeconds()).isEqualTo(30L);
    }

    @Test
    void newFields_setters() {
        properties.setSupportedLocales(List.of("zh_CN", "en_US"));
        properties.setCompositeOrder(List.of(SourceType.DATABASE, SourceType.PROPERTIES));
        properties.setCacheSeconds(600);
        properties.setReloadStrategy(ReloadStrategy.WATCH);
        properties.getDatabase().setTableName("i18n_msg");
        properties.getDatabase().setLocaleColumn("lang");
        properties.getDatabase().setCodeColumn("msg_code");
        properties.getDatabase().setMessageColumn("msg_text");

        assertThat(properties.getSupportedLocales()).containsExactly("zh_CN", "en_US");
        assertThat(properties.getCompositeOrder()).containsExactly(SourceType.DATABASE, SourceType.PROPERTIES);
        assertThat(properties.getCacheSeconds()).isEqualTo(600L);
        assertThat(properties.getReloadStrategy()).isEqualTo(ReloadStrategy.WATCH);
        assertThat(properties.getDatabase().getTableName()).isEqualTo("i18n_msg");
        assertThat(properties.getDatabase().getLocaleColumn()).isEqualTo("lang");
        assertThat(properties.getDatabase().getCodeColumn()).isEqualTo("msg_code");
        assertThat(properties.getDatabase().getMessageColumn()).isEqualTo("msg_text");
    }

    @Test
    void allSettersTogether() {
        properties.setEnabled(false);
        properties.setSource(SourceType.DATABASE);
        properties.setBasename("custom.messages");
        properties.setResolver(ResolverType.ACCEPT_HEADER);
        properties.setFormatter(FormatterType.NAMED_PARAMETER);
        properties.setDefaultLocale("en_US");
        properties.setReloadIntervalSeconds(60);

        assertThat(properties.isEnabled()).isFalse();
        assertThat(properties.getSource()).isEqualTo(SourceType.DATABASE);
        assertThat(properties.getBasename()).isEqualTo("custom.messages");
        assertThat(properties.getResolver()).isEqualTo(ResolverType.ACCEPT_HEADER);
        assertThat(properties.getFormatter()).isEqualTo(FormatterType.NAMED_PARAMETER);
        assertThat(properties.getDefaultLocale()).isEqualTo("en_US");
        assertThat(properties.getReloadIntervalSeconds()).isEqualTo(60L);
    }
}
