package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.properties.RateLimitAlgorithm;
import org.jspecify.annotations.NullMarked;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器实例管理器：按 维度 / 算法 / 阈值 缓存 {@link RateLimiter} 实例。
 *
 * <p>内置四种内存算法：固定窗口、滑动窗口、漏桶、令牌桶（默认）。
 * 多实例部署可注入基于 Redis 的 {@link RateLimiter} 实现以共享计数。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class RateLimiterManager {

    /** DEFAULT_FACTORY 常量。 */
    private static final RateLimiterFactory DEFAULT_FACTORY = RateLimiterManager::createByAlgorithm;

    /** cache 不可变字段。 */
    private final Map<String, RateLimiter> cache = new ConcurrentHashMap<>();
    /** factory 不可变字段。 */
    private final RateLimiterFactory factory;
    /** defaultAlgorithm 不可变字段。 */
    private final RateLimitAlgorithm defaultAlgorithm;

    /**
     * 构造实例。
     */
    public RateLimiterManager() {
        this(RateLimitAlgorithm.TOKEN_BUCKET);
    }

    /**
     * 构造实例。
     * @param defaultAlgorithm 参数 defaultAlgorithm
     */
    public RateLimiterManager(RateLimitAlgorithm defaultAlgorithm) {
        this(defaultAlgorithm, DEFAULT_FACTORY);
    }

    /**
     * 构造实例。
     * @param defaultAlgorithm 参数 defaultAlgorithm
     * @param factory 参数 factory
     */
    public RateLimiterManager(RateLimitAlgorithm defaultAlgorithm, RateLimiterFactory factory) {
        this.defaultAlgorithm = defaultAlgorithm;
        this.factory = factory;
    }

    /**
     * 获取（或创建）指定维度的限流器，使用默认算法。
     *
     * @param name    维度名
     * @param permits 窗口内允许的最大请求数
     * @param window  时间窗口（秒）
     * @return 限流器
     */
    public RateLimiter get(String name, int permits, int window) {
        return get(name, permits, window, defaultAlgorithm);
    }

    /**
     * 获取（或创建）指定维度与算法的限流器。
     *
     * @param name      维度名
     * @param permits   窗口内允许的最大请求数
     * @param window    时间窗口（秒）
     * @param algorithm 限流算法
     * @return 限流器
     */
    public RateLimiter get(String name, int permits, int window, RateLimitAlgorithm algorithm) {
        String key = name + ":" + algorithm + ":" + permits + ":" + window;
        RateLimiter existing = cache.get(key);
        if (existing != null) {
            return existing;
        }
        RateLimiter created = factory.create(permits, window, algorithm);
        RateLimiter prev = cache.putIfAbsent(key, created);
        return prev == null ? created : prev;
    }

    private static RateLimiter createByAlgorithm(int permits, int window, RateLimitAlgorithm algorithm) {
        return switch (algorithm) {
            case FIXED_WINDOW -> new FixedWindowRateLimiter(permits, window);
            case SLIDING_WINDOW -> new SlidingWindowRateLimiter(permits, window);
            case LEAKY_BUCKET -> new LeakyBucketRateLimiter(permits, window);
            case TOKEN_BUCKET -> new TokenBucketRateLimiter(permits, window);
        };
    }

    /** 限流器工厂函数。 */
    @FunctionalInterface
    public interface RateLimiterFactory {
        /**
         * @param permits   窗口内允许的最大请求数
         * @param window    时间窗口（秒）
         * @param algorithm 限流算法
         * @return 限流器
         */
        RateLimiter create(int permits, int window, RateLimitAlgorithm algorithm);
    }
}
