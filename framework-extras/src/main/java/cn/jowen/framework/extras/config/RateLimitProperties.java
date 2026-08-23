package cn.jowen.framework.extras.config;

import org.jspecify.annotations.NullMarked;

/**
 * 接口限流配置属性。
 *
 * <p>通过 {@code framework.extras.ratelimit.*} 前缀绑定，由 boot-autoconfigure 统一装配。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class RateLimitProperties {

    /** 是否启用限流，默认 true。 */
    private boolean enabled = true;

    /** 默认算法（fixed-window / sliding-window / leaky-bucket / token-bucket）。 */
    private String defaultAlgorithm = "sliding-window";

    /** Key 前缀，默认 "ratelimit:"。 */
    private String keyPrefix = "ratelimit:";

    /** 限流失败时的降级消息。 */
    private String fallbackMessage = "请求过于频繁，请稍后重试";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getDefaultAlgorithm() { return defaultAlgorithm; }
    public void setDefaultAlgorithm(String defaultAlgorithm) { this.defaultAlgorithm = defaultAlgorithm; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public String getFallbackMessage() { return fallbackMessage; }
    public void setFallbackMessage(String fallbackMessage) { this.fallbackMessage = fallbackMessage; }
}
