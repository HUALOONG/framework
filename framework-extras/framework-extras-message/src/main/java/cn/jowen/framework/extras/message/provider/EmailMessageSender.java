package cn.jowen.framework.extras.message.provider;

import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.common.util.Assert;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageSender;
import cn.jowen.framework.extras.message.core.MessageType;
import org.jspecify.annotations.NullMarked;

/**
 * 邮件消息发送器（骨架，需业务方实现）。
 *
 * <p><b>定位说明</b>：本模块 {@code framework-extras-message} 定位为
 * <b>零外部依赖</b> 的核心抽象层，不引入 JavaMail / Angus Mail / Jakarta Mail
 * 等邮件依赖，因此框架不内置具体实现，由<b>业务方按需实现</b>
 * {@link #doSend(String, String, String)}。
 *
 * <p><b>参考实现路径</b>：
 * <ul>
 *   <li>推荐做法：新建独立子模块（如 {@code framework-extras-message-email}）
 *       引入 Angus Mail + Jakarta Mail API，实现此类；</li>
 *   <li>或直接使用 Spring Boot 的 {@code spring-boot-starter-mail}。</li>
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
