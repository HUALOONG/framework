package cn.jowen.framework.boot.autoconfigure.observability;

import cn.jowen.framework.plugin.PluginManager;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * 可观测性装配：汇总框架运行指标（缓存命中率由缓存装配提供 MeterBinder），并暴露插件信息端点。
 *
 * <p>仅当 spring-boot-actuator 在 classpath 且容器存在 {@link PluginManager} 时生效。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@AutoConfiguration(afterName = "cn.jowen.framework.boot.autoconfigure.plugin.PluginAutoConfiguration")
@ConditionalOnClass(Endpoint.class)
@ConditionalOnBean(PluginManager.class)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PluginEndpoint frameworkPluginEndpoint(PluginManager pluginManager) {
        return new PluginEndpoint(pluginManager);
    }
}
