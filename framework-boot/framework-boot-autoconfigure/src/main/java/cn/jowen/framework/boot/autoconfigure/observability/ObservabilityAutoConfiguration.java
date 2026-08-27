package cn.jowen.framework.boot.autoconfigure.observability;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 可观测性集中总闸：统一声明框架指标能力的启用门槛与配置入口。
 *
 * <p>条件：Micrometer 在 classpath 且 {@code framework.observability.enabled}（缺省开启）。
 * 绑定 {@link BootObservabilityProperties}（{@code framework.observability.*}）供业务注入；
 * 各模块指标（cache 的 {@code MeterBinder}、jdbc 的 SQL 耗时 Timer）仍按各自条件装配，
 * Spring Boot 4 自动将 {@code MeterBinder} bean 绑定到 {@link MeterRegistry}。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "framework.observability", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(BootObservabilityProperties.class)
public class ObservabilityAutoConfiguration {
}