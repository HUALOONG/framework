package cn.jowen.framework.boot.autoconfigure.extras;

import cn.jowen.framework.extras.common.exception.NotificationException;
import cn.jowen.framework.extras.message.core.Message;
import cn.jowen.framework.extras.message.core.MessageResult;
import cn.jowen.framework.extras.message.core.MessageService;
import cn.jowen.framework.extras.message.core.MessageType;
import cn.jowen.framework.extras.web.captcha.SmsCaptchaSender;
import org.jspecify.annotations.NullMarked;

/**
 * 短信验证码默认发送实现：桥接 {@code framework-extras-message} 的 {@link MessageService}。
 *
 * <p><b>存在意义</b>：{@code SmsCaptchaGenerator} 依赖 {@link SmsCaptchaSender} 下发验证码，
 * 但框架不绑定任何短信服务商，此前无默认实现，导致短信验证码必须业务方自行接入才可用。
 * 本类复用已有的 message 模块（其 {@code SmsMessageSender} 由业务方按渠道实现），
 * 使引入 message 模块的应用开箱即用。
 *
 * <p><b>为什么放在装配层</b>：{@code framework-extras-message} 是 optional 依赖，
 * 本类必须位于 autoconfigure，由 {@code @ConditionalOnClass} / {@code @ConditionalOnBean}
 * 双重保护，确保未引入 message 模块时不参与装配。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class MessageServiceSmsCaptchaSender implements SmsCaptchaSender {

    /** 内容模板占位符：验证码 */
    private static final String PLACEHOLDER = "{code}";

    /** messageService 不可变字段。 */
    private final MessageService messageService;
    /** template 不可变字段。 */
    private final String template;
    /** title 不可变字段。 */
    private final String title;

    /**
     * 创建发送器。
     *
     * @param messageService 消息服务，不可为 {@code null}
     * @param template       内容模板，需包含 {@code {code}} 占位符
     * @param title          消息标题
     */
    public MessageServiceSmsCaptchaSender(MessageService messageService, String template, String title) {
        this.messageService = messageService;
        this.template = template;
        this.title = title;
    }

    /**
     * 使用默认模板创建发送器。
     *
     * @param messageService 消息服务
     */
    public MessageServiceSmsCaptchaSender(MessageService messageService) {
        this(messageService, "您的验证码是：" + PLACEHOLDER + "，请勿向任何人透露。", "验证码");
    }

    /**
     * 执行send操作。
     * @param phone 参数 phone
     * @param code 参数 code
     */
    @Override
    public void send(String phone, String code) {
        Message message = Message.builder(MessageType.SMS)
                .receiver(phone)
                .title(title)
                .content(template.replace(PLACEHOLDER, code))
                .build();

        MessageResult result = messageService.send(message);
        if (result == null || !result.successful()) {
            throw new NotificationException("短信验证码发送失败"
                    + (result == null || result.error() == null ? "" : "：" + result.error()));
        }
    }
}
