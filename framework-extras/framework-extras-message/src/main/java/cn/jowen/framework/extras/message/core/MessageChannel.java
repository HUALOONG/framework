package cn.jowen.framework.extras.message.core;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 消息渠道路由：根据消息类型选择对应 {@link MessageSender}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface MessageChannel {

    /**
     * 发送消息，内部按类型路由到合适的发送器。
     *
     * @param message 消息内容
     */
    void dispatch(Message message);

    /**
     * 返回当前已注册的发送器列表。
     *
     * @return 发送器列表
     */
    List<MessageSender> senders();
}
