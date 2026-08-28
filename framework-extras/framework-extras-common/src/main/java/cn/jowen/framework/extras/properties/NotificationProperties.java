package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 消息通知配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
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
