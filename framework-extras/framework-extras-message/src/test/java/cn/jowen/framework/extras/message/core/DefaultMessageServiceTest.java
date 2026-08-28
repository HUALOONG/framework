package cn.jowen.framework.extras.message.core;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.template.SimpleTemplateEngine;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DefaultMessageService} 门面验证：重试、批量、模板渲染。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
class DefaultMessageServiceTest {

    private static final Executor DIRECT = Runnable::run;

    /** 可配置失败次数的发送器，用于验证重试 */
    private static final class FlakySender implements MessageSender {
        private final AtomicInteger attempts = new AtomicInteger();
        private final int failTimes;

        FlakySender(int failTimes) {
            this.failTimes = failTimes;
        }

        @Override
        public MessageType supportedType() {
            return MessageType.SMS;
        }

        @Override
        public void send(Message message) {
            if (attempts.getAndIncrement() < failTimes) {
                throw new ExtrasException("boom");
            }
        }
    }

    private static Message sms(String content) {
        return Message.builder(MessageType.SMS).receiver("13800000000")
                .title("t").content(content).build();
    }

    @Test
    void sendReturnsSuccess() {
        MessageService service = new DefaultMessageService(
                new DefaultMessageChannel(List.of(new FlakySender(0))),
                new SimpleTemplateEngine(), DIRECT, false, 0);

        assertThat(service.send(sms("hi")).successful()).isTrue();
    }

    @Test
    void sendRetriesUntilMaxThenFails() {
        MessageService service = new DefaultMessageService(
                new DefaultMessageChannel(List.of(new FlakySender(99))),
                new SimpleTemplateEngine(), DIRECT, false, 2);

        MessageResult result = service.send(sms("hi"));
        assertThat(result.successful()).isFalse();
        assertThat(result.error()).contains("boom");
    }

    @Test
    void sendSucceedsAfterOneRetry() {
        MessageService service = new DefaultMessageService(
                new DefaultMessageChannel(List.of(new FlakySender(1))),
                new SimpleTemplateEngine(), DIRECT, false, 3);

        assertThat(service.send(sms("hi")).successful()).isTrue();
    }

    @Test
    void sendBatchPreservesOrder() {
        MessageService service = new DefaultMessageService(
                new DefaultMessageChannel(List.of(new FlakySender(0))),
                new SimpleTemplateEngine(), DIRECT, false, 0);

        List<MessageResult> results = service.sendBatch(List.of(sms("a"), sms("b"), sms("c")));
        assertThat(results).hasSize(3).allMatch(MessageResult::successful);
    }

    @Test
    void sendAsyncCompletesSuccessfully() throws Exception {
        MessageService service = new DefaultMessageService(
                new DefaultMessageChannel(List.of(new FlakySender(0))),
                new SimpleTemplateEngine(), DIRECT, true, 0);

        CompletableFuture<MessageResult> future = service.sendAsync(sms("hi"));
        assertThat(future.get().successful()).isTrue();
    }

    @Test
    void sendTemplateRendersVariables() {
        MessageService service = new DefaultMessageService(
                new DefaultMessageChannel(List.of(new FlakySender(0))),
                new SimpleTemplateEngine(), DIRECT, false, 0);

        MessageTemplate template = new MessageTemplate(MessageType.SMS,
                "你好 ${name}", "验证码 ${code}");

        assertThat(service.sendTemplate(template, "13800000000",
                Map.of("name", "张三", "code", "1234")).successful()).isTrue();
    }
}
