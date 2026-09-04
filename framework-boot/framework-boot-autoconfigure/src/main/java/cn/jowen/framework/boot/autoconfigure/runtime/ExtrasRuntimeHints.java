package cn.jowen.framework.boot.autoconfigure.runtime;

import cn.jowen.framework.extras.message.provider.DingTalkMessageSender;
import cn.jowen.framework.extras.message.provider.EmailMessageSender;
import cn.jowen.framework.extras.message.provider.PushMessageSender;
import cn.jowen.framework.extras.message.provider.SiteMessageSender;
import cn.jowen.framework.extras.message.provider.SmsMessageSender;
import cn.jowen.framework.extras.message.provider.WebhookMessageSender;
import cn.jowen.framework.extras.message.provider.WeComMessageSender;
import org.jspecify.annotations.NullMarked;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * GraalVM 原生镜像提示：extras 模块的消息发送器（MessageSender 模板方法子类，按需反射实例化）。
 *
 * <p>集中登记于 {@code boot-autoconfigure}：extras 基础模块保持 Spring 无关，无法各自承载 AOT registrar。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class ExtrasRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        MemberCategory[] sender = {
                MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
                MemberCategory.INVOKE_PUBLIC_METHODS
        };
        hints.reflection().registerType(DingTalkMessageSender.class, sender);
        hints.reflection().registerType(EmailMessageSender.class, sender);
        hints.reflection().registerType(PushMessageSender.class, sender);
        hints.reflection().registerType(SiteMessageSender.class, sender);
        hints.reflection().registerType(SmsMessageSender.class, sender);
        hints.reflection().registerType(WebhookMessageSender.class, sender);
        hints.reflection().registerType(WeComMessageSender.class, sender);
    }
}
