package cn.jowen.framework.extras.message.core;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.exception.ErrorCodeEnum;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认消息渠道：按 {@link MessageType} 路由到对应 {@link MessageSender}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DefaultMessageChannel implements MessageChannel {

    private final Map<MessageType, MessageSender> senderMap = new ConcurrentHashMap<>();

    public DefaultMessageChannel(List<MessageSender> senders) {
        for (MessageSender sender : senders) {
            senderMap.put(sender.supportedType(), sender);
        }
    }

    @Override
    public void dispatch(Message message) {
        MessageSender sender = senderMap.get(message.getType());
        if (sender == null) {
            throw new ExtrasException(ErrorCodeEnum.OPERATION_REJECTED,
                    "未注册消息类型对应的发送器: " + message.getType());
        }
        sender.send(message);
    }

    @Override
    public List<MessageSender> senders() {
        return List.copyOf(senderMap.values());
    }
}
