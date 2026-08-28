package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 推送消息发送器（骨架）。
 *
 * <p>接入具体推送服务（APP / 微信模板消息等）时，实现 {@link #doSend(String, String, String)} 即可。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class PushMessageSender implements MessageSender {

    @Override
    public final MessageType supportedType() {
        return MessageType.PUSH;
    }

    @Override
    public final void send(Message message) {
        Assert.hasText(message.getReceiver(), "推送接收标识不能为空");
        Assert.hasText(message.getContent(), "推送内容不能为空");
        try {
            doSend(message.getReceiver(), message.getTitle(), message.getContent());
        } catch (ExtrasException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtrasException(cn.jowen.framework.extras.common.exception.ErrorCodeEnum.INTERNAL_ERROR, e);
        }
    }

    protected abstract void doSend(String receiver, String title, String content);
}
