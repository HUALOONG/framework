package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 幂等控制配置属性。
 *
 * <p>通过 {@code framework.extras.idempotent.*} 前缀绑定，由 boot-autoconfigure 统一装配。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class IdempotentProperties {

    /** 是否启用幂等控制，默认 true。 */
    private boolean enabled = true;

    /** 默认 TTL（毫秒），默认 60s。 */
    private long defaultTtl = 60_000;

    /** Key 前缀，默认 "idempotent:"。 */
    private String keyPrefix = "idempotent:";

    /** Token 模式下的请求头名称，默认 "X-Idempotent-Token"。 */
    private String tokenHeader = "X-Idempotent-Token";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getDefaultTtl() { return defaultTtl; }
    public void setDefaultTtl(long defaultTtl) { this.defaultTtl = defaultTtl; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getTokenHeader() { return tokenHeader; }
    public void setTokenHeader(String tokenHeader) { this.tokenHeader = tokenHeader; }
}
