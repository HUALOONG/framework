package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SiteMessageSender} 骨架契约。
 */
class SiteMessageSenderTest {

    private static final class Stub extends SiteMessageSender {
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

    private static Message site(String receiver, String title, String content) {
        return Message.builder(MessageType.SITE).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsSite() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.SITE);
    }

    @Test
    void sendRejectsBlankReceiver() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(site("  ", "标题", "正文")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("站内信接收人不能为空");
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(site("u1", "标题", "")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("站内信内容不能为空");
    }

    @Test
    void sendInvokesDoSendWithReceiverTitleContent() {
        Stub stub = new Stub();
        stub.send(site("u1", "通知", "请及时查看"));

        assertThat(stub.receiver).isEqualTo("u1");
        assertThat(stub.title).isEqualTo("通知");
        assertThat(stub.content).isEqualTo("请及时查看");
    }

    @Test
    void sendWrapsNonExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new RuntimeException("db fail");

        assertThatThrownBy(() -> stub.send(site("u1", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(site("u1", "t", "c"))).doesNotThrowAnyException();
    }
}
