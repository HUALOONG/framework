package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 各渠道发送器骨架的统一异常契约：{@code doSend} 抛出的 {@link ExtrasException} 必须原样透传，
 * 不得被外层 catch 二次包装。
 *
 * <p>二次包装会让调用方丢失业务错误码（只剩 {@code INTERNAL_ERROR}），无法区分
 * "渠道拒收"与"网络故障"。SMS 渠道的同型契约见 {@link SmsMessageSenderTest}，本类补齐其余 6 个渠道。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class MessageSenderExtrasExceptionPropagationTest {

    /** 统一注入的业务异常：所有渠道共用，便于断言"原样透传而非包装"。 */
    private static final ExtrasException BIZ_REJECTED =
            new ExtrasException("渠道拒收：内容含敏感词");

    /** 记录调用参数并支持注入异常的三参测试桩。 */
    private static final class ThreeArgStub extends PushMessageSender {
        @Override
        protected void doSend(String receiver, String title, String content) {
            throw BIZ_REJECTED;
        }
    }

    private static Message messageOf(MessageSender sender, String receiver, String title, String content) {
        return Message.builder(sender.supportedType())
                .receiver(receiver)
                .title(title)
                .content(content)
                .build();
    }

    @Test
    void push_propagatesExtrasException() {
        ThreeArgStub stub = new ThreeArgStub();

        assertThatThrownBy(() -> stub.send(messageOf(stub, "dev-1", "提醒", "正文")))
                .isSameAs(BIZ_REJECTED);
    }

    @Test
    void email_propagatesExtrasException() {
        MessageSender stub = new EmailMessageSender() {
            @Override
            protected void doSend(String to, String subject, String body) {
                throw BIZ_REJECTED;
            }
        };

        assertThatThrownBy(() -> stub.send(messageOf(stub, "a@b.com", "主题", "正文")))
                .isSameAs(BIZ_REJECTED);
    }

    @Test
    void site_propagatesExtrasException() {
        MessageSender stub = new SiteMessageSender() {
            @Override
            protected void doSend(String receiver, String title, String content) {
                throw BIZ_REJECTED;
            }
        };

        assertThatThrownBy(() -> stub.send(messageOf(stub, "user-1", "标题", "正文")))
                .isSameAs(BIZ_REJECTED);
    }

    @Test
    void dingTalk_propagatesExtrasException() {
        MessageSender stub = new DingTalkMessageSender() {
            @Override
            protected void doSend(String webhook, String title, String content) {
                throw BIZ_REJECTED;
            }
        };

        assertThatThrownBy(() -> stub.send(messageOf(stub, "https://oapi.dingtalk.com/robot/send", "标题", "正文")))
                .isSameAs(BIZ_REJECTED);
    }

    @Test
    void weCom_propagatesExtrasException() {
        MessageSender stub = new WeComMessageSender() {
            @Override
            protected void doSend(String webhook, String title, String content) {
                throw BIZ_REJECTED;
            }
        };

        assertThatThrownBy(() -> stub.send(messageOf(stub, "https://qyapi.weixin.qq.com/cgi-bin/webhook/send", "标题", "正文")))
                .isSameAs(BIZ_REJECTED);
    }

    @Test
    void webhook_propagatesExtrasException() {
        MessageSender stub = new WebhookMessageSender() {
            @Override
            protected void doSend(String url, String title, String content) {
                throw BIZ_REJECTED;
            }
        };

        assertThatThrownBy(() -> stub.send(messageOf(stub, "https://hooks.example.com/hook", "标题", "正文")))
                .isSameAs(BIZ_REJECTED);
    }
}
