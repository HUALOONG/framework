package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 站内信发送器（骨架，需业务方实现）。
 *
 * <p><b>定位说明</b>：站内信需与业务方数据库 Schema、用户 ID 空间、
 * 未读状态模型强耦合，无通用契约可抽。本模块 {@code framework-extras-message}
 * 定位为<b>零外部依赖</b> 的核心抽象层，因此框架不内置具体实现，
 * 由<b>业务方按需实现</b> {@link #doSend(String, String, String)}。
 *
 * <p><b>参考实现路径</b>：
 * <ul>
 *   <li>推荐做法：新建独立子模块（如 {@code framework-extras-message-site}）
 *       引入 MyBatis / JPA 落库，或对接 Kafka/RabbitMQ 异步投递；</li>
 *   <li>或直接使用 Spring Data JPA + Redis 未读标记的常见组合。</li>
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
