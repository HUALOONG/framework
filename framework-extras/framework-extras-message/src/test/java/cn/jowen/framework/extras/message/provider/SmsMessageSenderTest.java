package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SmsMessageSender} 骨架契约：类型、校验、doSend 参数、异常传播与包装。
 */
class SmsMessageSenderTest {

    /** 记录调用并支持注入异常的测试桩 */
    private static final class Stub extends SmsMessageSender {
        String phone;
        String content;
        RuntimeException toThrow;

        @Override
        protected void doSend(String phone, String content) {
            if (toThrow != null) {
                throw toThrow;
            }
            this.phone = phone;
            this.content = content;
        }
    }

    private static Message sms(String receiver, String content) {
        return Message.builder(MessageType.SMS).receiver(receiver).title("t").content(content).build();
    }

    @Test
    void supportedTypeIsSms() {
        assertThat(new Stub().supportedType()).isEqualTo(MessageType.SMS);
    }

    @Test
    void sendRejectsBlankReceiver() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(sms("  ", "内容")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("短信接收号码不能为空");
    }

    @Test
    void sendRejectsBlankContent() {
        Stub stub = new Stub();
        assertThatThrownBy(() -> stub.send(sms("13800000000", "")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("短信内容不能为空");
    }

    @Test
    void sendInvokesDoSendWithPhoneAndContent() {
        Stub stub = new Stub();
        stub.send(sms("13800000000", "验证码1234"));

        assertThat(stub.phone).isEqualTo("13800000000");
        assertThat(stub.content).isEqualTo("验证码1234");
    }

    @Test
    void sendPropagatesExtrasExceptionFromDoSend() {
        Stub stub = new Stub();
        stub.toThrow = new ExtrasException("网关拒绝");

        assertThatThrownBy(() -> stub.send(sms("13800000000", "x")))
                .isInstanceOf(ExtrasException.class)
                .hasMessageContaining("网关拒绝");
    }

    @Test
    void sendWrapsNonExtrasExceptionAsExtrasException() {
        Stub stub = new Stub();
        stub.toThrow = new IllegalStateException("连接超时");

        assertThatThrownBy(() -> stub.send(sms("13800000000", "x")))
                .isInstanceOf(ExtrasException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void validMessageDoesNotThrow() {
        Stub stub = new Stub();
        assertThatCode(() -> stub.send(sms("13800000000", "x"))).doesNotThrowAnyException();
    }
}
