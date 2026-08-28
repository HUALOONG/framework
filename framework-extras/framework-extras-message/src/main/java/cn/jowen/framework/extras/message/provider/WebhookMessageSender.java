package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ErrorCodeEnum;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 通用 Webhook 消息发送器（骨架）。
 *
 * <p>接入任意 HTTP 回调时，实现 {@link #doSend(String, String, String)} 即可。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public abstract class WebhookMessageSender implements MessageSender {

    /**
     * 执行supported type操作。
     * @return 结果
     */
    @Override
    public final MessageType supportedType() {
        return MessageType.WEBHOOK;
    }

    /**
     * 执行send操作。
     * @param message 参数 message
     */
    @Override
    public final void send(Message message) {
        Assert.hasText(message.getReceiver(), "Webhook 地址不能为空");
        Assert.hasText(message.getContent(), "Webhook 内容不能为空");
        try {
            doSend(message.getReceiver(), message.getTitle(), message.getContent());
        } catch (ExtrasException e) {
            throw e;
        } catch (Exception e) {
            throw new ExtrasException(ErrorCodeEnum.INTERNAL_ERROR, e);
        }
    }

    /**
     * 执行 Webhook 发送。
     *
     * @param url    回调地址
     * @param title  标题
     * @param content 内容
     */
    protected abstract void doSend(String url, String title, String content);
}
