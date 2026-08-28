package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.common.exception.NotificationException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageResult;
import cn.jowen.framework.extras.message.core.MessageService;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MessageServiceSmsCaptchaSender} 单元测试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class MessageServiceSmsCaptchaSenderTest {

    private final MessageService messageService = mock(MessageService.class);

    @Test
    void sendsSmsMessageWithRenderedCode() {
        when(messageService.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(MessageResult.success("task-1"));
        MessageServiceSmsCaptchaSender sender =
                new MessageServiceSmsCaptchaSender(messageService);

        sender.send("13800138000", "1234");

        var captor = forClass(Message.class);
        org.mockito.Mockito.verify(messageService).send(captor.capture());
        Message message = captor.getValue();

        assertThat(message.getType()).isEqualTo(MessageType.SMS);
        assertThat(message.getReceiver()).isEqualTo("13800138000");
        assertThat(message.getContent()).contains("1234");
    }

    @Test
    void usesCustomTemplateAndTitle() {
        when(messageService.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(MessageResult.success());
        MessageServiceSmsCaptchaSender sender = new MessageServiceSmsCaptchaSender(
                messageService, "【测试】code={code}", "登录验证");

        sender.send("13800138000", "9999");

        var captor = forClass(Message.class);
        org.mockito.Mockito.verify(messageService).send(captor.capture());
        Message message = captor.getValue();

        assertThat(message.getContent()).isEqualTo("【测试】code=9999");
        assertThat(message.getTitle()).isEqualTo("登录验证");
    }

    @Test
    void throwsWhenSendFails() {
        when(messageService.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(MessageResult.failure("余额不足"));
        MessageServiceSmsCaptchaSender sender =
                new MessageServiceSmsCaptchaSender(messageService);

        assertThatThrownBy(() -> sender.send("13800138000", "1234"))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("余额不足");
    }

    @Test
    void throwsWhenResultIsNull() {
        when(messageService.send(org.mockito.ArgumentMatchers.any())).thenReturn(null);
        MessageServiceSmsCaptchaSender sender =
                new MessageServiceSmsCaptchaSender(messageService);

        assertThatThrownBy(() -> sender.send("13800138000", "1234"))
                .isInstanceOf(NotificationException.class)
                .hasMessageContaining("短信验证码发送失败");
    }

    @Test
    void doesNotThrowOnSuccess() {
        when(messageService.send(org.mockito.ArgumentMatchers.any()))
                .thenReturn(MessageResult.success());
        MessageServiceSmsCaptchaSender sender =
                new MessageServiceSmsCaptchaSender(messageService);

        assertThatCode(() -> sender.send("13800138000", "1234")).doesNotThrowAnyException();
    }
}
