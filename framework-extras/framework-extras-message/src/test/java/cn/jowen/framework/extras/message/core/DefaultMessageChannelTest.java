package cn.jowen.framework.extras.message.core;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DefaultMessageChannel} 路由验证。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class DefaultMessageChannelTest {

    /** 记录已发送消息的测试用发送器 */
    private static final class RecordingSender implements MessageSender {
        private final MessageType type;
        private final List<Message> sent = new ArrayList<>();

        RecordingSender(MessageType type) {
            this.type = type;
        }

        @Override
        public MessageType supportedType() {
            return type;
        }

        @Override
        public void send(Message message) {
            sent.add(message);
        }
    }

    private static Message messageOf(MessageType type) {
        return Message.builder(type).receiver("u").title("t").content("c").build();
    }

    @Test
    void dispatchRoutesToMatchingSender() {
        RecordingSender sms = new RecordingSender(MessageType.SMS);
        RecordingSender email = new RecordingSender(MessageType.EMAIL);
        MessageChannel channel = new DefaultMessageChannel(List.of(sms, email));

        Message message = messageOf(MessageType.EMAIL);
        channel.dispatch(message);

        assertThat(email.sent).containsExactly(message);
        assertThat(sms.sent).isEmpty();
    }

    @Test
    void dispatchThrowsWhenNoSenderRegistered() {
        MessageChannel channel = new DefaultMessageChannel(List.of());
        assertThatThrownBy(() -> channel.dispatch(messageOf(MessageType.WEBHOOK)))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("未注册消息类型对应的发送器");
    }

    @Test
    void sendersExposesAllRegistered() {
        RecordingSender a = new RecordingSender(MessageType.SMS);
        RecordingSender b = new RecordingSender(MessageType.PUSH);
        assertThat(new DefaultMessageChannel(List.of(a, b)).senders()).hasSize(2);
    }
}
