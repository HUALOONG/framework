package cn.jowen.framework.boot.autoconfigure.logger;

import cn.jowen.framework.logger.config.LoggerProperties;
import cn.jowen.framework.logger.mask.LogMasker;
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
 * {@link LoggerAutoConfiguration} 集成测试。
 *
 * @author 王飞
 * @since 2026-08-26
 */
class LoggerAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LoggerAutoConfiguration.class))
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
            .withPropertyValues("framework.logger.format=text");

    @Test
    void shouldCreateLoggerBootstrapBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(LoggerBootstrap.class);
        });
    }

    @Test
    void shouldCreateLogMaskerBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(LogMasker.class);
        });
    }

    @Test
    void shouldBindLoggerProperties() {
        context.run(ctx -> {
            LoggerProperties props = ctx.getBean(LoggerProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.isAsyncEnabled()).isTrue();
            assertThat(props.getLevel()).isEqualTo("debug");
            assertThat(props.getFormat()).isEqualTo("text");
            assertThat(props.isDesensitizeEnabled()).isTrue();
        });
    }

    @Test
    void shouldNotActivateWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(LoggerAutoConfiguration.class))
                .withPropertyValues("framework.logger.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(LoggerBootstrap.class);
                    assertThat(ctx).doesNotHaveBean(LogMasker.class);
                });
    }
}
