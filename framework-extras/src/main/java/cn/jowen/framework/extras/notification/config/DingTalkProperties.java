package cn.jowen.framework.extras.notification.config;

import org.jspecify.annotations.NullMarked;

/**
 * 钉钉通知渠道配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.notification.dingtalk}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class DingTalkProperties {

    /**
     * 钉钉应用 AppKey。
     */
    private String appKey;

    /**
     * 钉钉应用 AppSecret。
     */
    private String appSecret;

    /**
     * 自定义 Webhook 地址（机器人 webhook，用于简单文本消息）。
     */
    private String webhookUrl;

    /**
     * 消息类型：text / markdown。
     */
    private String msgType = "text";

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }
}
