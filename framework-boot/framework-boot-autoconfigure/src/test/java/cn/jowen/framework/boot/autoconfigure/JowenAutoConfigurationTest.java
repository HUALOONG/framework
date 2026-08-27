package cn.jowen.framework.boot.autoconfigure;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.extras.lock.Lock;
import cn.jowen.framework.i18n.api.MessageSource;
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
 * {@link JowenAutoConfiguration} 集成测试：验证总装配入口正确聚合所有子装配类。
 *
 * @author 王飞
 * @since 2026-08-26
 */
class JowenAutoConfigurationTest {

    @Test
    void shouldLoadAllSubConfigurations() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JowenAutoConfiguration.class))
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
                .withPropertyValues("framework.data.type=jdbc")
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheManager.class);
                    assertThat(ctx).hasSingleBean(LogMasker.class);
                    assertThat(ctx).hasSingleBean(MessageSource.class);
                    assertThat(ctx).hasSingleBean(Lock.class);
                });
    }

    @Test
    void shouldNotLoadWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(JowenAutoConfiguration.class))
                .withPropertyValues("framework.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(CacheManager.class);
                });
    }
}