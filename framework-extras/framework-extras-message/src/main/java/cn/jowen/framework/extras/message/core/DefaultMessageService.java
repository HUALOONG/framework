package cn.jowen.framework.extras.message.core;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.message.template.Template;
import cn.jowen.framework.extras.message.template.TemplateEngine;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * {@link MessageService} 默认实现：基于 {@link MessageChannel} 路由，
 * 可选异步执行器与失败重试。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DefaultMessageService implements MessageService {

    /** channel 不可变字段。 */
    private final MessageChannel channel;
    /** templateEngine 不可变字段。 */
    private final TemplateEngine templateEngine;
    /** executor 不可变字段。 */
    private final Executor executor;
    /** async 不可变字段。 */
    private final boolean async;
    /** maxRetries 不可变字段。 */
    private final int maxRetries;

    /**
     * 构造实例。
     * @param channel 参数 channel
     * @param templateEngine 参数 templateEngine
     * @param executor 参数 executor
     * @param async 参数 async
     * @param maxRetries 参数 maxRetries
     */
    public DefaultMessageService(MessageChannel channel, TemplateEngine templateEngine,
                                 Executor executor, boolean async, int maxRetries) {
        this.channel = channel;
        this.templateEngine = templateEngine;
        this.executor = executor;
        this.async = async;
        this.maxRetries = Math.max(0, maxRetries);
    }

    /**
     * 执行send操作。
     * @param message 参数 message
     * @return 结果
     */
    @Override
    public MessageResult send(Message message) {
        RuntimeException last = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                channel.dispatch(message);
                return MessageResult.success();
            } catch (ExtrasException e) {
                last = e;
            }
        }
        return MessageResult.failure(last == null ? "发送失败" : last.getMessage());
    }

    /**
     * 执行send async操作。
     * @param message 参数 message
     * @return 结果
     */
    @Override
    public CompletableFuture<MessageResult> sendAsync(Message message) {
        return CompletableFuture.supplyAsync(() -> send(message), executor);
    }

    /**
     * 执行send batch操作。
     * @return 结果
     */
    @Override
    public List<MessageResult> sendBatch(List<Message> messages) {
        return messages.stream().map(this::send).toList();
    }

    /**
     * 执行send template操作。
     * @param template 参数 template
     * @param receiver 参数 receiver
     * @return 结果
     */
    @Override
    public MessageResult sendTemplate(MessageTemplate template, String receiver,
                                      Map<String, Object> variables) {
        var rendered = templateEngine.render(
                new Template(template.type().name(), template.titleTemplate(), template.contentTemplate()),
                variables);
        Message message = Message.builder(template.type())
                .receiver(receiver)
                .title(rendered.title())
                .content(rendered.content())
                .build();
        return send(message);
    }
}
