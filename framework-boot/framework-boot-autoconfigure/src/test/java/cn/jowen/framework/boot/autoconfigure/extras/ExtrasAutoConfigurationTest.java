package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import cn.jowen.framework.extras.properties.CaptchaProperties;
import cn.jowen.framework.extras.properties.DataPermissionProperties;
import cn.jowen.framework.extras.properties.DataScope;
import cn.jowen.framework.extras.properties.DesensitizeProperties;
import cn.jowen.framework.extras.properties.ExcelProperties;
import cn.jowen.framework.extras.properties.IdempotentProperties;
import cn.jowen.framework.extras.properties.Ip2RegionProperties;
import cn.jowen.framework.extras.properties.LockProperties;
import cn.jowen.framework.extras.properties.NotificationProperties;
import cn.jowen.framework.extras.properties.OperateLogProperties;
import cn.jowen.framework.extras.properties.RateLimitProperties;
import cn.jowen.framework.extras.properties.StorageProperties;
import cn.jowen.framework.extras.web.ratelimit.RateLimiterManager;
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
 * {@link ExtrasAutoConfiguration} 集成测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExtrasAutoConfigurationTest {

    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ExtrasAutoConfiguration.class))
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
            });

    @Configuration
    static class CacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new DefaultCacheManager();
        }
    }

    @Test
    void shouldCreateRateLimiterManager() {
        context.withUserConfiguration(CacheConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(RateLimiterManager.class);
                });
    }

    @Test
    void shouldNotCreateRateLimiterManagerWithoutCacheManager() {
        context.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(RateLimiterManager.class);
        });
    }

    @Test
    void shouldBindCoreProperties() {
        context.run(ctx -> {
            BootExtrasProperties props = ctx.getBean(BootExtrasProperties.class);
            assertThat(props.isEnabled()).isTrue();

            LockProperties lock = props.getLock();
            assertThat(lock.isEnabled()).isTrue();
            assertThat(lock.getKeyPrefix()).isEqualTo("lock:");
            assertThat(lock.getDefaultLeaseTime()).isEqualTo(30000L);

            RateLimitProperties ratelimit = props.getRatelimit();
            assertThat(ratelimit.isEnabled()).isTrue();

            IdempotentProperties idempotent = props.getIdempotent();
            assertThat(idempotent.isEnabled()).isTrue();

            CaptchaProperties captcha = props.getCaptcha();
            assertThat(captcha.getLength()).isEqualTo(4);
            assertThat(captcha.getWidth()).isEqualTo(120);
            assertThat(captcha.getHeight()).isEqualTo(40);
            assertThat(captcha.getExpireSeconds()).isEqualTo(120L);

            StorageProperties storage = props.getStorage();
            assertThat(storage.isEnabled()).isTrue();

            NotificationProperties notification = props.getNotification();
            assertThat(notification.isEnabled()).isTrue();

            ExcelProperties excel = props.getExcel();
            assertThat(excel.isEnabled()).isTrue();

            Ip2RegionProperties ip2region = props.getIp2region();
            assertThat(ip2region.isEnabled()).isTrue();

            DesensitizeProperties desensitize = props.getDesensitize();
            assertThat(desensitize.isEnabled()).isTrue();

            OperateLogProperties operatelog = props.getOperatelog();
            assertThat(operatelog.isEnabled()).isTrue();
            assertThat(operatelog.isAsync()).isTrue();

            DataPermissionProperties datapermission = props.getDatapermission();
            assertThat(datapermission.isEnabled()).isTrue();
            assertThat(datapermission.getDefaultScope()).isEqualTo(DataScope.ALL);
        });
    }

    @Test
    void shouldNotActivateWhenDisabled() {
        context.withPropertyValues("framework.extras.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RateLimiterManager.class);
                });
    }
}
