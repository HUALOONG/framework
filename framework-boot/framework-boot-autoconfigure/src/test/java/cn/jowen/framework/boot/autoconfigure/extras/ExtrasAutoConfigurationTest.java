package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.cache.api.DefaultCacheManager;
import cn.jowen.framework.extras.config.CaptchaProperties;
import cn.jowen.framework.extras.config.DataPermissionProperties;
import cn.jowen.framework.extras.config.DesensitizeProperties;
import cn.jowen.framework.extras.config.ExcelProperties;
import cn.jowen.framework.extras.config.IdempotentProperties;
import cn.jowen.framework.extras.config.Ip2RegionProperties;
import cn.jowen.framework.extras.config.LockProperties;
import cn.jowen.framework.extras.config.NotificationProperties;
import cn.jowen.framework.extras.config.OperateLogProperties;
import cn.jowen.framework.extras.config.RateLimitProperties;
import cn.jowen.framework.extras.config.StorageProperties;
import cn.jowen.framework.extras.datapermission.DataScope;
import cn.jowen.framework.extras.idempotent.Idempotent;
import cn.jowen.framework.extras.lock.Lock;
import cn.jowen.framework.extras.lock.LockType;
import cn.jowen.framework.extras.lock.LocalLock;
import cn.jowen.framework.extras.ratelimit.RateLimiter;
import cn.jowen.framework.extras.ratelimit.RateLimiterManager;
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
 * @since 2026-08-26
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
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(RateLimiterManager.class);
        });
    }

    @Test
    void shouldCreateRateLimiter() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(RateLimiter.class);
        });
    }

    @Test
    void shouldCreateLocalLock() {
        context.run(ctx -> {
            assertThat(ctx).hasSingleBean(Lock.class);
            assertThat(ctx.getBean(Lock.class)).isInstanceOf(LocalLock.class);
        });
    }

    @Test
    void shouldCreateIdempotentWithCacheManager() {
        context.withUserConfiguration(CacheConfig.class)
                .run(ctx -> {
                    assertThat(ctx).hasSingleBean(Idempotent.class);
                });
    }

    @Test
    void shouldNotCreateIdempotentWithoutCacheManager() {
        context.run(ctx -> {
            assertThat(ctx).doesNotHaveBean(Idempotent.class);
        });
    }

    @Test
    void shouldBindAllSubProperties() {
        context.run(ctx -> {
            BootExtrasProperties props = ctx.getBean(BootExtrasProperties.class);
            assertThat(props.isEnabled()).isTrue();

            LockProperties lock = props.getLock();
            assertThat(lock.isEnabled()).isTrue();
            assertThat(lock.getType()).isEqualTo(LockType.REENTRANT);
            assertThat(lock.getKeyPrefix()).isEqualTo("lock:");
            assertThat(lock.getDefaultLeaseTime()).isEqualTo(30000);
            assertThat(lock.getDefaultWaitTime()).isEqualTo(10000);
            assertThat(lock.isWatchdogEnabled()).isFalse();

            RateLimitProperties ratelimit = props.getRatelimit();
            assertThat(ratelimit.isEnabled()).isTrue();
            assertThat(ratelimit.getDefaultAlgorithm()).isEqualTo("sliding-window");
            assertThat(ratelimit.getKeyPrefix()).isEqualTo("ratelimit:");
            assertThat(ratelimit.getFallbackMessage()).isEqualTo("请求过于频繁，请稍后重试");

            IdempotentProperties idempotent = props.getIdempotent();
            assertThat(idempotent.isEnabled()).isTrue();
            assertThat(idempotent.getDefaultTtl()).isEqualTo(60000);
            assertThat(idempotent.getKeyPrefix()).isEqualTo("idempotent:");
            assertThat(idempotent.getTokenHeader()).isEqualTo("X-Idempotent-Token");

            CaptchaProperties captcha = props.getCaptcha();
            assertThat(captcha.getLength()).isEqualTo(4);
            assertThat(captcha.getWidth()).isEqualTo(120);
            assertThat(captcha.getHeight()).isEqualTo(40);
            assertThat(captcha.getTtlMillis()).isEqualTo(300000);
            assertThat(captcha.isCaseSensitive()).isFalse();
            assertThat(captcha.getCharSet()).isEqualTo("ABCDEFGHJKLMNPQRSTUVWXYZ23456789");

            StorageProperties storage = props.getStorage();
            assertThat(storage.getRootLocation()).isEqualTo("./storage");
            assertThat(storage.getNamingStrategy()).isEqualTo("date");
            assertThat(storage.isGeneratePresignedUrl()).isFalse();

            NotificationProperties notification = props.getNotification();
            assertThat(notification.getDefaultFrom()).isEqualTo("noreply@jowen.cn");
            assertThat(notification.isAsyncEnabled()).isTrue();

            ExcelProperties excel = props.getExcel();
            assertThat(excel.isEnabled()).isTrue();
            assertThat(excel.getDefaultFileName()).isEqualTo("report");
            assertThat(excel.getDefaultSheetName()).isEqualTo("Sheet1");
            assertThat(excel.getImportBatchSize()).isEqualTo(500);
            assertThat(excel.getHeaderRowCount()).isEqualTo(1);

            Ip2RegionProperties ip2region = props.getIp2region();
            assertThat(ip2region.isEnabled()).isTrue();
            assertThat(ip2region.getDbPath()).isEqualTo("ip2region.xdb");
            assertThat(ip2region.getLoadType()).isEqualTo(Ip2RegionProperties.LoadType.MEMORY);
            assertThat(ip2region.getTrustedHeaders()).containsExactly("X-Forwarded-For", "X-Real-IP");

            DesensitizeProperties desensitize = props.getDesensitize();
            assertThat(desensitize.isEnabled()).isTrue();
            assertThat(desensitize.isFailOnUnknownStrategy()).isFalse();

            OperateLogProperties operatelog = props.getOperatelog();
            assertThat(operatelog.isEnabled()).isTrue();
            assertThat(operatelog.isAsync()).isTrue();

            DataPermissionProperties datapermission = props.getDatapermission();
            assertThat(datapermission.isEnabled()).isTrue();
            assertThat(datapermission.getDefaultScope()).isEqualTo(DataScope.ALL);
            assertThat(datapermission.getDefaultDeptColumn()).isEqualTo("dept_id");
            assertThat(datapermission.getDefaultUserColumn()).isEqualTo("create_by");
            assertThat(datapermission.getIgnoreTables()).isEmpty();
        });
    }

    @Test
    void shouldNotActivateWhenDisabled() {
        context.withPropertyValues("framework.extras.enabled=false")
                .run(ctx -> {
                    assertThat(ctx).doesNotHaveBean(RateLimiterManager.class);
                    assertThat(ctx).doesNotHaveBean(Lock.class);
                });
    }
}
