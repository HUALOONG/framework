package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.boot.autoconfigure.BootAutoConfiguration;
import cn.jowen.framework.i18n.api.LocaleResolver;
import cn.jowen.framework.i18n.api.MessageSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 国际化装配集成测试：验证空配置下消息源与区域解析器自动装配，并支持多语言解析。
 */
class I18nAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class));

    @Test
    void messageSourceAndResolverRegistered() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(MessageSource.class);
            assertThat(context).hasSingleBean(LocaleResolver.class);
        });
    }

    @Test
    void resolvesMessageByLocale() {
        runner.run(context -> {
            MessageSource source = context.getBean(MessageSource.class);
            assertThat(source.getMessage("app.name", Locale.ENGLISH, null)).isEqualTo("Framework");
            assertThat(source.getMessage("app.name", new Locale("zh", "CN"), null)).isEqualTo("框架");
        });
    }

    @Test
    void localeResolverReadsParam() {
        runner.run(context -> {
            LocaleResolver resolver = context.getBean(LocaleResolver.class);
            Locale resolved = resolver.resolve(Map.of("lang", "zh_CN"));
            assertThat(resolved).isEqualTo(new Locale("zh", "CN"));
        });
    }

    @Test
    void i18nDisabledByProperty() {
        runner.withPropertyValues("framework.i18n.enabled=false").run(context ->
                assertThat(context).doesNotHaveBean(MessageSource.class));
    }
}
