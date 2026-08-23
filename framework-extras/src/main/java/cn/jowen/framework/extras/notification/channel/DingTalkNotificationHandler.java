package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import cn.jowen.framework.extras.notification.config.DingTalkProperties;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

/**
 * 钉钉通知渠道处理器（骨架实现）。
 *
 * <p>支持两种模式：
 * <ul>
 *   <li><b>自定义 Webhook 模式</b>：通过 {@code webhookUrl} 发送文本/Markdown 消息。</li>
 *   <li><b>企业应用模式</b>：通过 {@code appKey}/{@code appSecret} 获取 AccessToken，调用企业应用消息 API（待接入）。</li>
 * </ul>
 * 依赖 {@code com.dingtalk.open:dingtalk-sdk}（optional）。
 *
 * @author 王飞
 * @since 2026-08-22
 * @see DingTalkProperties
 */
@NullMarked
public final class DingTalkNotificationHandler implements NotificationChannelHandler {

    private final DingTalkProperties props;

    public DingTalkNotificationHandler(DingTalkProperties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return channel == NotificationChannel.DINGTALK;
    }

    @Override
    public NotificationResult send(NotificationRequest req) {
        if (req.channel() != NotificationChannel.DINGTALK) {
            return NotificationResult.failure(NotificationChannel.DINGTALK, "不支持的渠道");
        }
        String url = req.to();
        if (url == null || url.isBlank()) {
            url = props.getWebhookUrl();
        }
        if (url == null || url.isBlank()) {
            return NotificationResult.failure(NotificationChannel.DINGTALK, "钉钉 webhookUrl 未配置");
        }
        String content = req.content();
        if (content == null || content.isBlank()) {
            content = req.subject() != null ? req.subject() : "";
        }

        // TODO: 集成钉钉 SDK
        // 简单 webhook 模式示例：
        // Map<String, Object> body = Map.of("msgtype", "text",
        //         "text", Map.of("content", content));
        // Request httpRequest = new Request(url)
        //         .setBody(JsonUtils.toJson(body))
        //         .setMethod(Request.METHOD_POST);
        // Client client = new DefaultClient();
        // client.execute(httpRequest);

        // 当前为骨架占位
        return NotificationResult.failure(NotificationChannel.DINGTALK,
                "钉钉 SDK 尚未接入（待集成 com.dingtalk.open:dingtalk-sdk）");
    }
}
