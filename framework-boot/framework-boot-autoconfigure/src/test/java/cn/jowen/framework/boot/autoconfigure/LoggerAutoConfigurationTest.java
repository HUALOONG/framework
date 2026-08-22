package cn.jowen.framework.boot.autoconfigure;

import cn.jowen.framework.boot.autoconfigure.logger.LoggerBootstrap;
import cn.jowen.framework.logger.config.LoggerProperties;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.logger.mask.LogMasker;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 日志装配集成测试：验证在最小 Spring 上下文中 framework-logger 自动装配生效。
 */
class LoggerAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BootAutoConfiguration.class));

    @Test
    void loggerAutoConfigurationRegistersBeans() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LoggerProperties.class);
            assertThat(context).hasSingleBean(LogMasker.class);
            assertThat(context).hasSingleBean(LoggerBootstrap.class);
            // 门面可在上下文启动后解析底层适配器
            assertThat(LoggerFactory.resolveAdapter()).isNotNull();
        });
    }

    @Test
    void loggerDisabledByProperty() {
        runner.withPropertyValues("framework.logger.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(LoggerBootstrap.class);
        });
    }
}
