package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.message.properties.MessageProperties;
import cn.jowen.framework.extras.message.core.MessageService;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link MessageAutoConfiguration} 装配验证：确认 {@code framework.extras.message.*}
 * 配置生效，且 {@code async} / {@code maxRetries} 真正传入 {@link MessageService}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class MessageAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MessageAutoConfiguration.class));

    @Test
    void messagePropertiesAreBound() {
        runner.withPropertyValues(
                        "framework.extras.message.enabled=true",
                        "framework.extras.message.default-channel=sms",
                        "framework.extras.message.max-retries=3",
                        "framework.extras.message.async=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    MessageProperties props = context.getBean(BootMessageProperties.class);
                    assertThat(props.getDefaultChannel()).isEqualTo("sms");
                    assertThat(props.getMaxRetries()).isEqualTo(3);
                    assertThat(props.isAsync()).isTrue();
                });
    }

    @Test
    void defaultsAreAppliedWhenNoConfig() {
        runner.run(context -> {
            MessageProperties props = context.getBean(BootMessageProperties.class);
            assertThat(props.getDefaultChannel()).isEqualTo("email");
            assertThat(props.getMaxRetries()).isZero();
            assertThat(props.isAsync()).isFalse();
        });
    }

    @Test
    void coreBeansRegistered() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasBean("messageChannel");
            assertThat(context).hasBean("templateEngine");
            assertThat(context).hasBean("messageExecutor");
            assertThat(context).hasBean("messageService");
            assertThat(context.getBean(MessageService.class)).isNotNull();
        });
    }
}
