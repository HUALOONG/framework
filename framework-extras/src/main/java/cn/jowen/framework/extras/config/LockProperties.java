package cn.jowen.framework.extras.config;

import cn.jowen.framework.extras.lock.LockType;
import org.jspecify.annotations.NullMarked;

/**
 * 分布式锁配置属性。
 *
 * <p>通过 {@code framework.extras.lock.*} 前缀绑定，由 boot-autoconfigure 统一装配。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class LockProperties {

    /** 是否启用分布式锁，默认 true。 */
    private boolean enabled = true;

    /** 锁类型，默认可重入锁。 */
    private LockType type = LockType.REENTRANT;

    /** Key 前缀，默认 "lock:"。 */
    private String keyPrefix = "lock:";

    /** 默认锁持有时间（毫秒），默认 30s。 */
    private long defaultLeaseTime = 30_000;

    /** 默认等待获取锁时间（毫秒），默认 10s。 */
    private long defaultWaitTime = 10_000;

    /** 是否启用 Watchdog 自动续期，默认 false。 */
    private boolean watchdogEnabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public LockType getType() { return type; }
    public void setType(LockType type) { this.type = type; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public long getDefaultLeaseTime() { return defaultLeaseTime; }
    public void setDefaultLeaseTime(long defaultLeaseTime) { this.defaultLeaseTime = defaultLeaseTime; }

    public long getDefaultWaitTime() { return defaultWaitTime; }
    public void setDefaultWaitTime(long defaultWaitTime) { this.defaultWaitTime = defaultWaitTime; }

    public boolean isWatchdogEnabled() { return watchdogEnabled; }
    public void setWatchdogEnabled(boolean watchdogEnabled) { this.watchdogEnabled = watchdogEnabled; }
}
