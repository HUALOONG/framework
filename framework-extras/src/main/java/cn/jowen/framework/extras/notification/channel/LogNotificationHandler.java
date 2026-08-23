package cn.jowen.framework.extras.notification.channel;

import cn.jowen.framework.extras.notification.NotificationChannel;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationRequest;
import cn.jowen.framework.extras.notification.NotificationResult;
import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * 日志通知处理器：将通知内容写入框架日志门面（{@link Logger}），用于本地调试。
 *
 * <p>零外部依赖，不真实发送。支持全部渠道（调试用途）。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class LogNotificationHandler implements NotificationChannelHandler {

    private final Logger logger = LoggerFactory.getLogger(LogNotificationHandler.class);

    @Override
    public NotificationResult send(NotificationRequest req) {
        String message = "channel=" + req.channel()
                + (req.to() != null ? ", to=" + req.to() : "")
                + (req.subject() != null ? ", subject=" + req.subject() : "")
                + ", content=" + req.content();
        logger.info("[NOTIFY] " + message);
        return NotificationResult.success(req.channel(), UUID.randomUUID().toString());
    }

    @Override
    public boolean supports(NotificationChannel channel) {
        return true;
    }
}
