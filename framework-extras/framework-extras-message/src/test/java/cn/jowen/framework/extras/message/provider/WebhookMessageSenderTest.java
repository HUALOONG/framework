package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link WebhookMessageSender} 骨架契约。
 */
class WebhookMessageSenderTest {

    private static final class Stub extends WebhookMessageSender {
        String url;
        String title;
        String content;
        RuntimeException toThrow;

        @Override
        protected void doSend(String url, String title, String content) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.url = url;
            this.title = title;
            this.content = content;
        }
    }

    private static Message webhook(String receiver, String title, String content) {
        return Message.builder(MessageType.WEBHOOK).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsWebhook() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.WEBHOOK);
    }

    @Test
    void sendRejectsBlankReceiver() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(webhook("  ", "标题", "正文")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("Webhook 地址不能为空");
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(webhook("https://hook", "标题", "")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("Webhook 内容不能为空");
    }

    @Test
    void sendInvokesDoSendWithUrlTitleContent() {
        Stub stub = new Stub();
        stub.send(webhook("https://example.com/cb", "事件", "触发了"));

        assertThat(stub.url).isEqualTo("https://example.com/cb");
        assertThat(stub.title).isEqualTo("事件");
        assertThat(stub.content).isEqualTo("触发了");
    }

    @Test
    void sendWrapsNonExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new RuntimeException("connect fail");

        assertThatThrownBy(() -> stub.send(webhook("https://hook", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(webhook("https://hook", "t", "c"))).doesNotThrowAnyException();
    }
}
