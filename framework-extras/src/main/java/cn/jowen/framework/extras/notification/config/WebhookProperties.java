package cn.jowen.framework.extras.notification.config;

import org.jspecify.annotations.NullMarked;

import java.util.Map;

/**
 * Webhook 通用通知渠道配置 POJO。
 *
 * <p>将通知内容以 HTTP POST JSON 方式发送到任意 URL；对应属性前缀 {@code framework.extras.notification.webhook}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public class WebhookProperties {

    /**
     * 默认 Base URL（NotificationRequest.to 可覆盖）。
     */
    private String baseUrl;

    /**
     * 请求超时毫秒（默认 5000）。
     */
    private long timeout = 5_000L;

    /**
     * 固定请求头（如 {@code Authorization}、自定义 Header）。
     */
    private Map<String, String> headers;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? Map.of() : headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
}
