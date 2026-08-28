package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 幂等控制配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class IdempotentProperties {

    /** enabled 字段。 */
    private boolean enabled = true;
    /** defaultExpireSeconds 字段。 */
    private long defaultExpireSeconds = 300L;

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
     * 获取default expire seconds。
     * @return 结果
     */
    public long getDefaultExpireSeconds() {
        return defaultExpireSeconds;
    }

    /**
     * 设置default expire seconds。
     * @param defaultExpireSeconds 参数 defaultExpireSeconds
     */
    public void setDefaultExpireSeconds(long defaultExpireSeconds) {
        this.defaultExpireSeconds = defaultExpireSeconds;
    }
}
