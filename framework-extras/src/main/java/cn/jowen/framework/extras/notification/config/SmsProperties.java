/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.notification.config;

import org.jspecify.annotations.NullMarked;

/**
 * 阿里云 SMS 通知渠道配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.notification.sms}。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class SmsProperties {

    /** 阿里云 AccessKey ID。 */
    private String accessKeyId;

    /** 阿里云 AccessKey Secret。 */
    private String accessKeySecret;

    /** 短信签名名称。 */
    private String signName;

    /** 默认模板编码（可在 NotificationRequest.templateCode 覆盖）。 */
    private String templateCode;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }

    public String getSignName() {
        return signName;
    }

    public void setSignName(String signName) {
        this.signName = signName;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }
}
