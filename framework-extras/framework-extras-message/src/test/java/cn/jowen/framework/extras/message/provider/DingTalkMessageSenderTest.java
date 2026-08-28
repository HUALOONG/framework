package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DingTalkMessageSender} 骨架契约（仅校验 content，receiver 可空）。
 */
class DingTalkMessageSenderTest {

    private static final class Stub extends DingTalkMessageSender {
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

    private static Message dingTalk(String receiver, String title, String content) {
        return Message.builder(MessageType.DINGTALK).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsDingTalk() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.DINGTALK);
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(dingTalk("https://hook", "标题", "")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("钉钉消息内容不能为空");
    }

    @Test
    void sendDoesNotRequireReceiver() {
        // 钉钉骨架不校验 receiver，空 receiver 也应进入 doSend
        Stub stub = new Stub();
        stub.send(dingTalk(null, "报警", "CPU 过高"));

        assertThat(stub.webhook).isNull();
        assertThat(stub.title).isEqualTo("报警");
        assertThat(stub.content).isEqualTo("CPU 过高");
    }

    @Test
    void sendInvokesDoSendWithWebhookTitleContent() {
        Stub stub = new Stub();
        stub.send(dingTalk("https://oapi.dingtalk.com/robot", "报警", "CPU 过高"));

        assertThat(stub.webhook).isEqualTo("https://oapi.dingtalk.com/robot");
        assertThat(stub.title).isEqualTo("报警");
        assertThat(stub.content).isEqualTo("CPU 过高");
    }

    @Test
    void sendWrapsNonExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new RuntimeException("http fail");

        assertThatThrownBy(() -> stub.send(dingTalk("https://hook", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(dingTalk("https://hook", "t", "c"))).doesNotThrowAnyException();
    }
}
