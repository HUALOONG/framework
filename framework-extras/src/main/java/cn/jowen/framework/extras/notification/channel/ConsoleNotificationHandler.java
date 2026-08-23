package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * 控制台通知处理器：将通知内容输出到 {@link System#out}，用于本地调试。
 *
 * <p>零外部依赖，不真实发送。支持全部渠道（调试用途）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class ConsoleNotificationHandler implements NotificationChannelHandler {

    @Override
    public NotificationResult send(NotificationRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("[NOTIFY] channel=").append(req.channel());
        if (req.to() != null) {
            sb.append(", to=").append(req.to());
        }
        if (req.subject() != null) {
            sb.append(", subject=").append(req.subject());
        }
        sb.append(", content=").append(req.content());
        System.out.println(sb);
        return NotificationResult.success(req.channel(), UUID.randomUUID().toString());
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return true;
    }
}
