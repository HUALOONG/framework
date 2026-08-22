package cn.jowen.framework.boot.autoconfigure.extras;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 扩展能力配置属性。绑定 {@code framework.extras.*}。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
@ConfigurationProperties(prefix = "framework.extras")
public class ExtrasProperties {

    /** 是否启用扩展能力装配，缺省开启。 */
    private boolean enabled = true;

    /** 限流器配置。 */
    private final RateLimit rateLimit = new RateLimit();

    /** 幂等控制配置。 */
    private final Idempotency idempotent = new Idempotency();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Idempotency getIdempotent() {
        return idempotent;
    }

    /** 令牌桶限流器参数。 */
    @NullMarked
    public static class RateLimit {

        /** 每秒许可数。 */
        private double permitsPerSecond = 100.0;

        /** 桶容量（突发上限）。 */
        private long capacity = 200;

        public double getPermitsPerSecond() {
            return permitsPerSecond;
        }

        public void setPermitsPerSecond(double permitsPerSecond) {
            this.permitsPerSecond = permitsPerSecond;
        }

        public long getCapacity() {
            return capacity;
        }

        public void setCapacity(long capacity) {
            this.capacity = capacity;
        }
    }

    /** 幂等控制参数。 */
    @NullMarked
    public static class Idempotency {

        /** 令牌缓存名。 */
        private String cacheName = "framework:idempotent";

        /** 令牌存活毫秒。 */
        private long ttlMillis = 60_000;

        public String getCacheName() {
            return cacheName;
        }

        public void setCacheName(String cacheName) {
            this.cacheName = cacheName;
        }

        public long getTtlMillis() {
            return ttlMillis;
        }

        public void setTtlMillis(long ttlMillis) {
            this.ttlMillis = ttlMillis;
        }
    }
}
