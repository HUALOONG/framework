package cn.jowen.framework.extras.properties;

import org.jspecify.annotations.NullMarked;

/**
 * 分布式锁配置。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class LockProperties {

    /** enabled 字段。 */
    private boolean enabled = true;
    /** type 字段。 */
    private LockType type = LockType.REENTRANT;
    /** keyPrefix 字段。 */
    private String keyPrefix = "lock:";
    /** defaultLeaseTime 字段。 */
    private long defaultLeaseTime = 30000L;
    /** watchdogEnabled 字段。 */
    private boolean watchdogEnabled = true;

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
     * 获取type。
     * @return 结果
     */
    public LockType getType() {
        return type;
    }

    /**
     * 设置type。
     * @param type 参数 type
     */
    public void setType(LockType type) {
        this.type = type;
    }

    /**
     * 获取key prefix。
     * @return 结果
     */
    public String getKeyPrefix() {
        return keyPrefix;
    }

    /**
     * 设置key prefix。
     * @param keyPrefix 参数 keyPrefix
     */
    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * 获取default lease time。
     * @return 结果
     */
    public long getDefaultLeaseTime() {
        return defaultLeaseTime;
    }

    /**
     * 设置default lease time。
     * @param defaultLeaseTime 参数 defaultLeaseTime
     */
    public void setDefaultLeaseTime(long defaultLeaseTime) {
        this.defaultLeaseTime = defaultLeaseTime;
    }

    /**
     * 获取watchdog enabled。
     * @return 结果
     */
    public boolean isWatchdogEnabled() {
        return watchdogEnabled;
    }

    /**
     * 设置watchdog enabled。
     * @param watchdogEnabled 参数 watchdogEnabled
     */
    public void setWatchdogEnabled(boolean watchdogEnabled) {
        this.watchdogEnabled = watchdogEnabled;
    }
}
