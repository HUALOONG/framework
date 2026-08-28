package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link EmailMessageSender} 骨架契约。
 */
class EmailMessageSenderTest {

    private static final class Stub extends EmailMessageSender {
        String to;
        String subject;
        String body;
        RuntimeException toThrow;

        @Override
        protected void doSend(String to, String subject, String body) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.to = to;
            this.subject = subject;
            this.body = body;
        }
    }

    private static Message email(String receiver, String title, String content) {
        return Message.builder(MessageType.EMAIL).receiver(receiver).title(title).content(content).build();
    }

    @Test
    void supportedTypeIsEmail() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.EMAIL);
    }

    @Test
    void sendRejectsBlankReceiver() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(email("  ", "标题", "正文")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("邮件接收地址不能为空");
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(email("a@b.com", "标题", "")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("邮件内容不能为空");
    }

    @Test
    void sendInvokesDoSendWithToSubjectBody() {
        Stub stub = new Stub();
        stub.send(email("a@b.com", "邀请函", "欢迎"));

        assertThat(stub.to).isEqualTo("a@b.com");
        assertThat(stub.subject).isEqualTo("邀请函");
        assertThat(stub.body).isEqualTo("欢迎");
    }

    @Test
    void sendWrapsNonExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new IllegalStateException("smtp down");

        assertThatThrownBy(() -> stub.send(email("a@b.com", "t", "c")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(email("a@b.com", "t", "c"))).doesNotThrowAnyException();
    }
}
