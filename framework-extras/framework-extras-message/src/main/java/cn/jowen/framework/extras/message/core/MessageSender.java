package cn.jowen.framework.extras.message.core;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import org.jspecify.annotations.NullMarked;

/**
 * 消息发送器，定义统一的发送契约。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface MessageSender {

    /**
     * 返回该发送器支持的消息类型。
     *
     * @return 消息类型
     */
    MessageType supportedType();

    /**
     * 发送消息。
     *
     * @param message 消息内容
     * @throws ExtrasException 发送失败时抛出
     */
    void send(Message message);
}
