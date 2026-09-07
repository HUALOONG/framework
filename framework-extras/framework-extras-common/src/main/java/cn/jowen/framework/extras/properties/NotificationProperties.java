package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 消息通知配置。
 *
 * <p><b>已废弃（N-003）。</b>该配置块处于"保留但不推荐使用"状态：框架的消息通知能力尚未落地，
 * 当前仅 {@code framework.extras.web.notification.enabled} 这一个开关被装配读取，且生产代码实际走的是
 * {@code MessageServiceSmsCaptchaSender} 直接桥接短信网关（引用 {@code NotificationException} 与协议级
 * {@code ErrorCodeEnum.NOTIFICATION_SEND_FAILED("E2004")}），并不依赖本配置。</p>
 *
 * <p>保留本类与 {@link cn.jowen.framework.extras.web.properties.ExtrasWebProperties.Notification} 仅为兼容既有
 * 占位结构，后续若正式启用通知能力再重构绑定关系。请勿在新代码中新增对本类的依赖。</p>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 * @deprecated 消息通知能力未落地，仅作占位保留；新代码请勿依赖本配置块。
 */
@Deprecated
@NullMarked
public class NotificationProperties {

    /** enabled 字段。 */
    private boolean enabled = true;
    /** defaultChannel 字段。 */
    private String defaultChannel = "email";

    /**
     * 获取enabled。
     * @return 结果
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置enabled。
     * @param enabled 参数 enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 获取default channel。
     * @return 结果
     */
    public String getDefaultChannel() {
        return defaultChannel;
    }

    /**
     * 设置default channel。
     * @param defaultChannel 参数 defaultChannel
     */
    public void setDefaultChannel(String defaultChannel) {
        this.defaultChannel = defaultChannel;
    }
}
