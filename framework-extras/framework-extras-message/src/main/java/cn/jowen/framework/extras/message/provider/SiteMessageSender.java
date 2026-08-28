package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 站内信发送器（骨架）。
 *
 * <p>具体实现可将消息落库或由消息中间件投递。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class SiteMessageSender implements MessageSender {

    @Override
    public final MessageType supportedType() {
        return MessageType.SITE;
    }

    @Override
    public final void send(Message message) {
        Assert.hasText(message.getReceiver(), "站内信接收人不能为空");
        Assert.hasText(message.getContent(), "站内信内容不能为空");
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
