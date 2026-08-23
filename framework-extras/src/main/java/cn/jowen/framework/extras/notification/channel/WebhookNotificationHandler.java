package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import cn.jowen.framework.extras.notification.config.WebhookProperties;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * Webhook 通用通知渠道处理器（基于 OkHttp）。
 *
 * <p>将通知内容以 HTTP POST JSON 方式发送到指定 URL；依赖 {@code com.squareup.okhttp3:okhttp}（optional）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class WebhookNotificationHandler implements NotificationChannelHandler {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.get("application/json; charset=utf-8");

    private final WebhookProperties props;
    private final OkHttpClient client;

    public WebhookNotificationHandler(WebhookProperties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(java.time.Duration.ofMillis(props.getTimeout()))
                .readTimeout(java.time.Duration.ofMillis(props.getTimeout()))
                .writeTimeout(java.time.Duration.ofMillis(props.getTimeout()));
        this.client = builder.build();
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WEBHOOK;
    }

    @Override
    public NotificationResult send(NotificationRequest req) {
        if (req.channel() != NotificationChannel.WEBHOOK) {
            return NotificationResult.failure(NotificationChannel.WEBHOOK, "不支持的渠道");
        }
        String url = resolveUrl(req);
        if (url == null || url.isBlank()) {
            return NotificationResult.failure(NotificationChannel.WEBHOOK, "webhook URL 未配置");
        }

        String body = req.content() != null ? req.content() : "{}";
        Request.Builder rb = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, JSON_MEDIA_TYPE));

        if (props.getHeaders() != null) {
            props.getHeaders().forEach(rb::addHeader);
        }

        try (Response resp = client.newCall(rb.build()).execute()) {
            int code = resp.code();
            if (code >= 200 && code < 300) {
                return NotificationResult.success(NotificationChannel.WEBHOOK, UUID.randomUUID().toString());
            }
            return NotificationResult.failure(NotificationChannel.WEBHOOK,
                    "HTTP " + code + " " + resp.message());
        } catch (IOException e) {
            return NotificationResult.failure(NotificationChannel.WEBHOOK, "网络异常: " + e.getMessage());
        }
    }

    private String resolveUrl(NotificationRequest req) {
        if (req.to() != null && !req.to().isBlank()) {
            return req.to();
        }
        return props.getBaseUrl();
    }
}
