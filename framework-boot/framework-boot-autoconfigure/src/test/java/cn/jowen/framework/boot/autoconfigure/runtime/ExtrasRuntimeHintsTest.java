package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.extras.message.provider.DingTalkMessageSender;
import cn.jowen.framework.extras.message.provider.EmailMessageSender;
import cn.jowen.framework.extras.message.provider.PushMessageSender;
import cn.jowen.framework.extras.message.provider.SiteMessageSender;
import cn.jowen.framework.extras.message.provider.SmsMessageSender;
import cn.jowen.framework.extras.message.provider.WebhookMessageSender;
import cn.jowen.framework.extras.message.provider.WeComMessageSender;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ExtrasRuntimeHints} 原生镜像提示测试。
 *
 * <p>既有 {@code RuntimeHintsTest} 覆盖了 cache/i18n/jdbc/logger/mybatis/spi 六类 registrar，
 * 但 extras 消息发送器这一类缺席。发送器为模板方法骨架、由装配层按配置反射实例化，
 * 若未登记 reflection hint，GraalVM 原生镜像下会静默退化为无发送能力。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExtrasRuntimeHintsTest {

    @Test
    void extrasRuntimeHints_registersAllMessageSenderTypes() {
        RuntimeHints hints = new RuntimeHints();

        new ExtrasRuntimeHints().registerHints(hints, null);

        assertThat(hints.reflection().getTypeHint(DingTalkMessageSender.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(EmailMessageSender.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(PushMessageSender.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(SiteMessageSender.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(SmsMessageSender.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(WebhookMessageSender.class)).isNotNull();
        assertThat(hints.reflection().getTypeHint(WeComMessageSender.class)).isNotNull();
    }

    @Test
    void extrasRuntimeHints_isIdempotentOnRepeatedRegistration() {
        RuntimeHints hints = new RuntimeHints();
        ExtrasRuntimeHints registrar = new ExtrasRuntimeHints();

        registrar.registerHints(hints, null);
        registrar.registerHints(hints, null);

        assertThat(hints.reflection().getTypeHint(SmsMessageSender.class)).isNotNull();
    }
}
