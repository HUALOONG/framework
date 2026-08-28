package cn.jowen.framework.extras.message.core;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息服务门面：统一消息发送入口。
 *
 * <p>屏蔽底层渠道差异，提供单条 / 批量 / 异步 / 模板化发送能力。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface MessageService {

    /**
     * 同步发送单条消息。
     *
     * @param message 消息
     * @return 发送结果
     */
    MessageResult send(Message message);

    /**
     * 异步发送单条消息。
     *
     * @param message 消息
     * @return 异步结果
     */
    CompletableFuture<MessageResult> sendAsync(Message message);

    /**
     * 批量发送消息。
     *
     * @param messages 消息列表
     * @return 与入参顺序一致的结果列表
     */
    List<MessageResult> sendBatch(List<Message> messages);

    /**
     * 按模板发送消息。
     *
     * @param template  模板
     * @param receiver  接收人
     * @param variables 模板变量
     * @return 发送结果
     */
    MessageResult sendTemplate(MessageTemplate template, String receiver, Map<String, Object> variables);
}
