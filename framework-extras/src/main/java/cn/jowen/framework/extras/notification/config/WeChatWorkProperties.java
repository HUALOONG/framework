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
 * 企业微信通知渠道配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.notification.wechat-work}。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class WeChatWorkProperties {

    /** 企业 ID。 */
    private String corpId;

    /** 应用 AgentId。 */
    private String agentId;

    /** 应用 Secret。 */
    private String secret;

    /** 自定义 Webhook 地址（用于简单文本消息）。 */
    private String webhookUrl;

    public String getCorpId() {
        return corpId;
    }

    public void setCorpId(String corpId) {
        this.corpId = corpId;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
