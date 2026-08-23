package cn.jowen.framework.extras.ratelimit;

import cn.jowen.framework.extras.ratelimit.algorithm.FixedWindowRateLimiter;
import cn.jowen.framework.extras.ratelimit.algorithm.LeakyBucketRateLimiter;
import cn.jowen.framework.extras.ratelimit.algorithm.SlidingWindowRateLimiter;
import cn.jowen.framework.extras.ratelimit.algorithm.TokenBucketRateLimiter;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器管理器。按算法类型创建限流器实例并缓存，支持按 key 获取或移除。
 *
 * <p>四种算法的纳秒周期换算：
 * <ul>
 *   <li>FIXED_WINDOW / SLIDING_WINDOW：period = 窗口周期</li>
 *   <li>LEAKY_BUCKET：drainInterval = period / permits（每次流出的间隔）</li>
 *   <li>TOKEN_BUCKET：refillInterval = period / permits（每次补充的间隔）</li>
 * </ul>
 */
@NullMarked
public final class RateLimiterManager {

    private final Map<String, RateLimiter> _limiterCache = new ConcurrentHashMap<>();

    public RateLimiterManager() {
    }

    /**
     * 获取或创建限流器。
     *
     * @param key       限流键
     * @param algorithm 限流算法
     * @param permits   窗口 / 桶容量（>0）
     * @param periodNanos 周期（纳秒，>0）
     * @return 限流器实例
     */
    public RateLimiter getLimiter(String key, RateLimitAlgorithm algorithm, int permits, long periodNanos) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (permits <= 0 || periodNanos <= 0) {
            throw new IllegalArgumentException("permits and periodNanos must be positive");
        }
        return _limiterCache.computeIfAbsent(key, k -> buildLimiter(algorithm, permits, periodNanos));
    }

    /**
     * 移除指定 key 的限流器。
     *
     * @param key 限流键
     */
    public void removeLimiter(String key) {
        _limiterCache.remove(key);
    }

    /**
     * @return 当前缓存的限流器数量
     */
    public int size() {
        return _limiterCache.size();
    }

    /**
     * 清除全部限流器。
     */
    public void clear() {
        _limiterCache.clear();
    }

    private static RateLimiter buildLimiter(RateLimitAlgorithm algorithm, int permits, long periodNanos) {
        return switch (algorithm) {
            case FIXED_WINDOW -> new FixedWindowRateLimiter(permits, periodNanos);
            case SLIDING_WINDOW -> new SlidingWindowRateLimiter(permits, periodNanos);
            case LEAKY_BUCKET -> new LeakyBucketRateLimiter(permits, periodNanos / Math.max(1, permits));
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(permits, periodNanos / Math.max(1, permits));
        };
    }
}