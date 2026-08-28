package cn.jowen.framework.extras.message.core;

import org.jspecify.annotations.NullMarked;

/**
 * 消息类型枚举。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public enum MessageType {
    /** 短信 */
    SMS,
    /** 邮件 */
    EMAIL,
    /** 站内信 */
    SITE,
    /** 推送（APP / 微信等） */
    PUSH,
    /** 钉钉机器人 */
    DINGTALK,
    /** 企业微信机器人 */
    WECOM,
    /** 自定义 Webhook */
    WEBHOOK
}
