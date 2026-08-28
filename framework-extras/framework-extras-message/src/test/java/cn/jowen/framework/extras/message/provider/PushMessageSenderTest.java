package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PushMessageSender} 骨架契约。
 */
class PushMessageSenderTest {

    private static final class Stub extends PushMessageSender {
        String receiver;
        String title;
        String content;
        RuntimeException toThrow;

        @Override
        protected void doSend(String receiver, String title, String content) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.receiver = receiver;
            this.title = title;
            this.content = content;
        }
    }

    private static Message push(String receiver, String title, String content) {
        return Message.builder(MessageType.PUSH).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsPush() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.PUSH);
    }

    @Test
    void sendRejectsBlankReceiver() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(push("", "标题", "正文")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("推送接收标识不能为空");
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(push("dev-1", "标题", "  ")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("推送内容不能为空");
    }

    @Test
    void sendInvokesDoSendWithReceiverTitleContent() {
        Stub stub = new Stub();
        stub.send(push("dev-1", "提醒", "有新消息"));

        assertThat(stub.receiver).isEqualTo("dev-1");
        assertThat(stub.title).isEqualTo("提醒");
        assertThat(stub.content).isEqualTo("有新消息");
    }

    @Test
    void sendWrapsNonExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new RuntimeException("push gateway fail");

        assertThatThrownBy(() -> stub.send(push("dev-1", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(push("dev-1", "t", "c"))).doesNotThrowAnyException();
    }
}
