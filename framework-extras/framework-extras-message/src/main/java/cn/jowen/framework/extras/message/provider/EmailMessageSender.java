package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 邮件消息发送器（骨架）。
 *
 * <p>接入具体邮件服务（SMTP / API）时，实现 {@link #doSend(String, String, String)} 即可。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class EmailMessageSender implements MessageSender {

    @Override
    public final MessageType supportedType() {
        return MessageType.EMAIL;
    }

    @Override
    public final void send(Message message) {
        Assert.hasText(message.getReceiver(), "邮件接收地址不能为空");
        Assert.hasText(message.getContent(), "邮件内容不能为空");
        try {
            doSend(message.getReceiver(), message.getTitle(), message.getContent());
        } catch (ExtrasException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtrasException(cn.jowen.framework.extras.common.exception.ErrorCodeEnum.INTERNAL_ERROR, e);
        }
    }

    /**
     * 执行邮件发送。
     *
     * @param to      接收地址
     * @param subject 主题
     * @param body    正文
     */
    protected abstract void doSend(String to, String subject, String body);
}
