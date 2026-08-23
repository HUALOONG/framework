package cn.jowen.framework.extras.notification;

import org.jspecify.annotations.NullMarked;

/**
 * 通知渠道枚举。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public enum NotificationChannel {
    EMAIL,
    SMS,
    DINGTALK,
    WECHAT_WORK,
    WEBHOOK,
    CUSTOM
}
