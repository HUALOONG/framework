package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 短信消息发送器（骨架）。
 *
 * <p>接入具体短信网关时，实现 {@link #doSend(String, String)} 即可。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class SmsMessageSender implements MessageSender {

    @Override
    public final MessageType supportedType() {
        return MessageType.SMS;
    }

    @Override
    public final void send(Message message) {
        Assert.hasText(message.getReceiver(), "短信接收号码不能为空");
        Assert.hasText(message.getContent(), "短信内容不能为空");
        try {
            doSend(message.getReceiver(), message.getContent());
        } catch (ExtrasException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtrasException(cn.jowen.framework.extras.common.exception.ErrorCodeEnum.INTERNAL_ERROR, e);
        }
    }

    /**
     * 执行短信发送。
     *
     * @param phone  接收号码
     * @param content 短信内容
     */
    protected abstract void doSend(String phone, String content);
}
