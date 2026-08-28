package cn.jowen.framework.boot.autoconfigure.cache;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CacheAutoConfiguration} 集成测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class CacheAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
            .withInitializer(ctx -> {
                YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
                yaml.setResources(new ClassPathResource("application.yaml"));
                Properties props = yaml.getObject();
                if (props != null) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    props.forEach((k, v) -> map.put(k.toString(), v));
                    ctx.getEnvironment().getPropertySources().addFirst(
                        new MapPropertySource("classpath:application.yaml", map));
                }
            });

    @Test
    void shouldCreateCacheManagerBean() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(CacheManager.class);
            assertThat(ctx.getBean(CacheManager.class)).isInstanceOf(DefaultCacheManager.class);
        });
    }

    @Test
    void shouldBindCacheProperties() {
        context.run(ctx -> {
            BootCacheProperties props = ctx.getBean(BootCacheProperties.class);
            assertThat(props.isEnabled()).isTrue();
            assertThat(props.isMetrics()).isTrue();
        });
    }

    @Test
    void shouldNotActivateWhenDisabled() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withPropertyValues("framework.cache.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(CacheManager.class);
                });
    }

    @Configuration
    static class CustomCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new DefaultCacheManager();
        }
    }

    @Test
    void shouldAllowCustomCacheManager() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
                .withUserConfiguration(CustomCacheConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(CacheManager.class);
                });
    }
}