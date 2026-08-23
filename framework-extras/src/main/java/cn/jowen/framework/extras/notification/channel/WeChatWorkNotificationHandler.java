package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import cn.jowen.framework.extras.notification.config.WeChatWorkProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * 企业微信通知渠道处理器（骨架实现）。
 *
 * <p>支持两种模式：
 * <ul>
 *   <li><b>自定义 Webhook 模式</b>：通过 {@code webhookUrl} 发送文本/Markdown 消息。</li>
 *   <li><b>企业应用模式</b>：通过 {@code corpId}/{@code agentId}/{@code secret} 获取 AccessToken 后发送应用消息（待接入）。</li>
 * </ul>
 * 依赖专用 SDK（optional）。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see WeChatWorkProperties
 */
@NullMarked
public final class WeChatWorkNotificationHandler implements NotificationChannelHandler {

    private final WeChatWorkProperties props;

    public WeChatWorkNotificationHandler(WeChatWorkProperties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.WECHAT_WORK;
    }

    @Override
    public NotificationResult send(NotificationRequest req) {
        if (req.channel() != NotificationChannel.WECHAT_WORK) {
            return NotificationResult.failure(NotificationChannel.WECHAT_WORK, "不支持的渠道");
        }
        String url = req.to();
        if (url == null || url.isBlank()) {
            url = props.getWebhookUrl();
        }
        if (url == null || url.isBlank()) {
            return NotificationResult.failure(NotificationChannel.WECHAT_WORK, "企业微信 webhookUrl 未配置");
        }
        String content = req.content();
        if (content == null || content.isBlank()) {
            content = req.subject() != null ? req.subject() : "";
        }

        // TODO: 集成企业微信 SDK
        // 简单 webhook 模式示例：
        // Map<String, Object> body = Map.of("msgtype", "text",
        //         "text", Map.of("content", content));
        // OkHttpClient client = new OkHttpClient();
        // Request request = new Request.Builder()
        //         .url(url)
        //         .post(RequestBody.create(JsonUtils.toJson(body), JSON))
        //         .build();
        // try (Response response = client.newCall(request).execute()) { ... }

        // 当前为骨架占位
        return NotificationResult.failure(NotificationChannel.WECHAT_WORK,
                "企业微信 SDK 尚未接入（待集成 workchat-sdk）");
    }
}
