package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 短信消息发送器（骨架，需业务方实现）。
 *
 * <p><b>定位说明</b>：本模块 {@code framework-extras-message} 定位为
 * <b>零外部依赖</b> 的核心抽象层，不包含任何第三方 SDK。短信发送依赖
 * 阿里云 SMS / 腾讯云 SMS / 华为云 SMS 等厂商 SDK，与零依赖定位冲突，
 * 因此框架不内置具体实现，由<b>业务方按需实现</b> {@link #doSend(String, String)}。
 *
 * <p><b>参考实现路径</b>：
 * <ul>
 *   <li>推荐做法：新建独立子模块（如 {@code framework-extras-message-sms}）
 *       引入厂商 SDK，实现此类；</li>
 *   <li>或使用 Spring Boot 的 {@code spring-cloud-alicloud-sms} 等 starter。</li>
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
