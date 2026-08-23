package cn.jowen.framework.boot.autoconfigure.logger;

import cn.jowen.framework.boot.autoconfigure.JowenAutoConfiguration;
import cn.jowen.framework.logger.config.LoggerProperties;
import cn.jowen.framework.logger.mask.LogMasker;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 日志能力装配。将门面 facade 桥接到底层实现（默认 Logback），并暴露脱敏开关与组件。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
@AutoConfiguration(after = JowenAutoConfiguration.class)
@ConditionalOnClass(name = "cn.jowen.framework.logger.facade.LoggerFactory")
@ConditionalOnProperty(prefix = "framework.logger", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(BootLoggerProperties.class)
public class LoggerAutoConfiguration {

    /**
     * 引导 facade → adapter 的解析。默认使用 Logback 适配器（classpath 存在时由 SPI 自动激活）。
     *
     * @param properties 日志属性，不可为 {@code null}
     * @return 引导 Bean
     */
    @Bean
    @ConditionalOnMissingBean
    public LoggerBootstrap loggerBootstrap(LoggerProperties properties) {
        return new LoggerBootstrap(properties);
    }

    /**
     * 日志脱敏组件，委托 core 脱敏器按配置开关工作。
     *
     * @param properties 日志属性，不可为 {@code null}
     * @return 脱敏器，不可为 {@code null}
     */
    @Bean
    @ConditionalOnMissingBean
    public LogMasker sensitiveDataMasker(LoggerProperties properties) {
        return new LogMasker(properties);
    }
}
