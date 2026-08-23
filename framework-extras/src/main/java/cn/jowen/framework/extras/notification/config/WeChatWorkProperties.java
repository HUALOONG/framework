package cn.jowen.framework.extras.notification.config;

import org.jspecify.annotations.NullMarked;

/**
 * 企业微信通知渠道配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.notification.wechat-work}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class WeChatWorkProperties {

    /**
     * 企业 ID。
     */
    private String corpId;

    /**
     * 应用 AgentId。
     */
    private String agentId;

    /**
     * 应用 Secret。
     */
    private String secret;

    /**
     * 自定义 Webhook 地址（用于简单文本消息）。
     */
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
