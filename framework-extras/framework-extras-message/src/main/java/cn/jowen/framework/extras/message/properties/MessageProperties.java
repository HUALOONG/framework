package cn.jowen.framework.extras.message.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 消息模块配置。
 *
 * <p>对应前缀 {@code framework.extras.message.*}。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class MessageProperties {

    /** 总开关 */
    private boolean enabled = true;
    /** 默认渠道名 */
    private String defaultChannel = "email";
    /** 发送失败最大重试次数 */
    private int maxRetries = 0;
    /** 是否启用异步发送 */
    private boolean async = false;

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

    /**
     * 获取max retries。
     * @return 结果
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * 设置max retries。
     * @param maxRetries 参数 maxRetries
     */
    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    /**
     * 获取async。
     * @return 结果
     */
    public boolean isAsync() {
        return async;
    }

    /**
     * 设置async。
     * @param async 参数 async
     */
    public void setAsync(boolean async) {
        this.async = async;
    }
}
