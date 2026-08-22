/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import cn.jowen.framework.extras.notification.config.SmsProperties;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NullMarked;

/**
 * 阿里云 SMS 通知渠道处理器（骨架实现）。
 *
 * <p>依赖 {@code com.aliyun:dysmsapi20170525}（optional）；未在 classpath 时由
 * {@code @ConditionalOnClass} 保证不注册。
 *
 * <p>集成点说明：
 * <ul>
 *   <li>使用 {@code com.aliyun:dysmsapi20170525} SDK 的 {@code SendSmsRequest} / {@code SendSmsResponse}。</li>
 *   <li>创建 Client 需传入 {@code accessKeyId}、{@code accessKeySecret} 及 endpoint（通常为 {@code dysmsapi.aliyuncs.com}）。</li>
 *   <li>模板参数通过 {@code NotificationRequest.templateParams} 传递，转换为 JSON 字符串传入 SDK。</li>
 * </ul>
 *
 * @author Jowen
 * @date 2026-08-22
 * @see SmsProperties
 */
@NullMarked
public final class SmsNotificationHandler implements NotificationChannelHandler {

    private final SmsProperties props;

    public SmsNotificationHandler(SmsProperties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.SMS;
    }

    @Override
    public NotificationResult send(NotificationRequest req) {
        if (req.channel() != NotificationChannel.SMS) {
            return NotificationResult.failure(NotificationChannel.SMS, "不支持的渠道");
        }
        if (props.getAccessKeyId() == null || props.getAccessKeyId().isBlank()) {
            return NotificationResult.failure(NotificationChannel.SMS, "sms.accessKeyId 未配置");
        }
        if (props.getAccessKeySecret() == null || props.getAccessKeySecret().isBlank()) {
            return NotificationResult.failure(NotificationChannel.SMS, "sms.accessKeySecret 未配置");
        }
        String phone = req.to();
        if (phone == null || phone.isBlank()) {
            return NotificationResult.failure(NotificationChannel.SMS, "手机号不能为空");
        }
        String templateCode = req.templateCode() != null && !req.templateCode().isBlank()
                ? req.templateCode()
                : props.getTemplateCode();
        if (templateCode == null || templateCode.isBlank()) {
            return NotificationResult.failure(NotificationChannel.SMS, "短信模板编码未配置");
        }

        // TODO: 集成阿里云 SMS SDK
        // 示例伪代码：
        // Client client = new Client(accessKeyId, accessKeySecret);
        // SendSmsRequest request = new SendSmsRequest()
        //     .setPhoneNumbers(phone)
        //     .setSignName(props.getSignName())
        //     .setTemplateCode(templateCode)
        //     .setTemplateParam(JsonUtils.toJson(req.templateParams()));
        // SendSmsResponse response = client.sendSms(request);
        // if ("OK".equals(response.getCode())) { ... }

        // 骨架占位：配置缺失时不会到达此处；配置完整时当前返回 success=false（等待 SDK 接入）
        return NotificationResult.failure(NotificationChannel.SMS,
                "阿里云 SMS SDK 尚未接入（待集成 com.aliyun:dysmsapi20170525）");
    }
}
