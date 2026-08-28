package cn.jowen.framework.boot.autoconfigure.health;

import cn.jowen.framework.cache.api.CacheManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 健康检查装配：注册框架健康指示器，供 actuator health 端点聚合。
 *
 * <p>仅当 spring-boot-health 在 classpath 且 {@code framework.health.enabled}（缺省开）时生效。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@AutoConfiguration
@ConditionalOnClass(HealthIndicator.class)
@ConditionalOnProperty(prefix = "framework.health", name = "enabled", matchIfMissing = true)
public class HealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "frameworkHealthIndicator")
    public BootHealthIndicator frameworkHealthIndicator(ApplicationContext context) {
        return new BootHealthIndicator(context);
    }

    /**
     * 缓存健康指示器（可选）。仅当框架 CacheManager bean 在容器中时装配。
     *
     * @param cacheManager 缓存管理器，不可为 {@code null}
     * @return 缓存健康指示器
     */
    @Bean
    @ConditionalOnBean(CacheManager.class)
    public CacheHealthIndicator cacheHealthIndicator(CacheManager cacheManager) {
        return new CacheHealthIndicator(cacheManager);
    }
}
