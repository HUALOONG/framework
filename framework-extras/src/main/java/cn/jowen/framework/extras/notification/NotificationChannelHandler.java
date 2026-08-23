package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;

/**
 * 通知渠道处理器（SPI）：处理特定渠道的发送逻辑。
 *
 * <p>实现应返回 {@link NotificationResult} 而非抛出异常；未知/失败场景返回 {@code success=false}。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public interface NotificationChannelHandler {

    /**
     * 发送通知。
     *
     * @param req 请求
     * @return 结果（禁止抛异常）
     */
    NotificationResult send(NotificationRequest req);

    /**
     * 是否支持该渠道。
     *
     * @param channel 渠道
     * @return 支持返回 {@code true}
     */
    boolean supports(NotificationChannel channel);
}
