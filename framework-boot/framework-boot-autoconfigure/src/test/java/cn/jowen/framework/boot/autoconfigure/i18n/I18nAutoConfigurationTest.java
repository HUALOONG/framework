package cn.jowen.framework.boot.autoconfigure.i18n;

import cn.jowen.framework.i18n.api.LocaleResolver;
import cn.jowen.framework.i18n.api.MessageSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link I18nAutoConfiguration} 集成测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class I18nAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(I18nAutoConfiguration.class))
            .withInitializer(ctx -> {
                YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
                yaml.setResources(new ClassPathResource("application.yaml"));
                Properties props = yaml.getObject();
                if (props != null) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    props.forEach((k, v) -> map.put(k.toString(), v));
                    ctx.getEnvironment().getPropertySources().addLast(
                        new MapPropertySource("classpath:application.yaml", map));
                }
            })
            .withPropertyValues(
                    "framework.i18n.basename=i18n/messages",
                    "framework.i18n.default-locale=zh_CN",
                    "framework.i18n.formatter=NAMED_PARAMETER"
            );

    @Test
    void shouldCreateMessageSourceBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(MessageSource.class);
            assertThat(ctx.getBean("frameworkMessageSource", MessageSource.class)).isNotNull();
        });
    }

    @Test
    void shouldCreateLocaleResolverBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(LocaleResolver.class);
            assertThat(ctx.getBean("frameworkLocaleResolver", LocaleResolver.class)).isNotNull();
        });
    }

    @Test
    void shouldBindI18nProperties() {
        context.run(ctx -> {
            BootI18nProperties props = ctx.getBean(BootI18nProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.getBasename()).isEqualTo("i18n/messages");
            assertThat(props.getDefaultLocale()).isEqualTo("zh_CN");
            assertThat(props.getReloadIntervalSeconds()).isEqualTo(0);
        });
    }

    @Test
    void shouldNotActivateWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(I18nAutoConfiguration.class))
                .withPropertyValues("framework.i18n.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean("frameworkMessageSource");
                    assertThat(ctx).doesNotHaveBean("frameworkLocaleResolver");
                });
    }
}