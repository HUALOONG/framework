package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link WeComMessageSender} 骨架契约（仅校验 content，receiver 可空）。
 */
class WeComMessageSenderTest {

    private static final class Stub extends WeComMessageSender {
        String webhook;
        String title;
        String content;
        RuntimeException toThrow;

        @Override
        protected void doSend(String webhook, String title, String content) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.webhook = webhook;
            this.title = title;
            this.content = content;
        }
    }

    private static Message wecom(String receiver, String title, String content) {
        return Message.builder(MessageType.WECOM).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsWeCom() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.WECOM);
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(wecom("https://hook", "标题", "  ")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("企业微信消息内容不能为空");
    }

    @Test
    void sendDoesNotRequireReceiver() {
        Stub stub = new Stub();
        stub.send(wecom(null, "周报", "请提交"));

        assertThat(stub.webhook).isNull();
        assertThat(stub.title).isEqualTo("周报");
        assertThat(stub.content).isEqualTo("请提交");
    }

    @Test
    void sendInvokesDoSendWithWebhookTitleContent() {
        Stub stub = new Stub();
        stub.send(wecom("https://qyapi.weixin.qq.com", "周报", "请提交"));

        assertThat(stub.webhook).isEqualTo("https://qyapi.weixin.qq.com");
        assertThat(stub.title).isEqualTo("周报");
        assertThat(stub.content).isEqualTo("请提交");
    }

    @Test
    void sendWrapsNonExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new RuntimeException("http fail");

        assertThatThrownBy(() -> stub.send(wecom("https://hook", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(wecom("https://hook", "t", "c"))).doesNotThrowAnyException();
    }
}
