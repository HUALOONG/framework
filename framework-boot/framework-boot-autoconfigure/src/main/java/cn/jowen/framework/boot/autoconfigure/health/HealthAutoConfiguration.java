package cn.jowen.framework.boot.autoconfigure.health;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * 健康检查装配：注册框架健康指示器，供 actuator health 端点聚合。
 *
 * <p>仅当 spring-boot-health 在 classpath 时生效。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration
@ConditionalOnClass(HealthIndicator.class)
public class HealthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "frameworkHealthIndicator")
    public BootHealthIndicator frameworkHealthIndicator(ApplicationContext context) {
        return new BootHealthIndicator(context);
    }
}
