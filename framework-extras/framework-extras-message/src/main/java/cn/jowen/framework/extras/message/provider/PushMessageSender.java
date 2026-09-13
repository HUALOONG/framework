package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 推送消息发送器（骨架，需业务方实现）。
 *
 * <p><b>定位说明</b>：推送渠道（iOS APNs / Android FCM / 厂商通道 / 微信模板消息等）
 * 协议差异极大，无统一 API 契约。本模块 {@code framework-extras-message}
 * 定位为<b>零外部依赖</b> 的核心抽象层，因此框架不内置具体实现，
 * 由<b>业务方按需实现</b> {@link #doSend(String, String, String)}。
 *
 * <p><b>参考实现路径</b>：
 * <ul>
 *   <li>推荐做法：按厂商各建一个独立子模块（如
 *       {@code framework-extras-message-push-fcm}、{@code ...-push-apns}）；</li>
 *   <li>或对接极光/个推等聚合推送平台后统一实现。</li>
 * </ul>
 *
 * <p>骨架仅提供参数校验与异常归一化，{@code doSend} 抛出的
 * {@link cn.jowen.framework.extras.common.exception.ExtrasException} 会原样透传，
 * 其他异常统一包装为 {@link cn.jowen.framework.extras.common.exception.ErrorCodeEnum#INTERNAL_ERROR}。
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
