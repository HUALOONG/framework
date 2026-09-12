package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.extras.properties.RateLimitAlgorithm;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 限流器实例管理器：按 维度 / 算法 / 阈值 缓存 {@link RateLimiter} 实例。
 *
 * <p>内置四种内存算法：固定窗口、滑动窗口、漏桶、令牌桶（默认）。
 * 当注入支持脚本的 {@link RedisCommandExecutor} 时，对支持集群化的算法
 * （{@code FIXED_WINDOW} / {@code TOKEN_BUCKET} / {@code SLIDING_WINDOW} / {@code LEAKY_BUCKET}）
 * 自动切换为 Redis 实现，使计数在多实例间共享；
 * 未提供执行器（或未提供 key 工具）时回落本地内存实现。
 *
 * <p>实现层保持零 Spring 依赖：Redis 执行器由装配层通过构造参数传入（与幂等 / 验证码范式一致）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.2
 */
@NullMarked
public final class RateLimiterManager {

    /** cache 不可变字段。 */
    private final Map<String, RateLimiter> cache = new ConcurrentHashMap<>();

    /** factory 不可变字段。 */
    private final RateLimiterFactory factory;

    /** defaultAlgorithm 不可变字段。 */
    private final RateLimitAlgorithm defaultAlgorithm;

    /** redisExecutor 可空字段：支持脚本时用于集群限流。 */
    private final @Nullable RedisCommandExecutor redisExecutor;

    /** keys 可空字段：Redis key 拼接工具。 */
    private final @Nullable RateLimitKeys keys;

    /** failOpen 不可变字段：Redis 异常时是否放行（默认 true）。 */
    private final boolean failOpen;

    /**
     * 构造实例（默认令牌桶、本地内存、fail-open）。
     */
    public RateLimiterManager() {
        this(RateLimitAlgorithm.TOKEN_BUCKET, null, null, true, null);
    }

    /**
     * 构造实例（指定默认算法，本地内存，fail-open）。
     * @param defaultAlgorithm 默认算法
     */
    public RateLimiterManager(RateLimitAlgorithm defaultAlgorithm) {
        this(defaultAlgorithm, null, null, true, null);
    }

    /**
     * 构造实例（指定默认算法与工厂，本地内存，fail-open）。
     * @param defaultAlgorithm 默认算法
     * @param factory 限流器工厂
     */
    public RateLimiterManager(RateLimitAlgorithm defaultAlgorithm, RateLimiterFactory factory) {
        this(defaultAlgorithm, null, null, true, factory);
    }

    /**
     * 构造实例（指定默认算法、Redis 执行器与 key 工具，fail-open）。
     *
     * <p>提供支持脚本的执行器时，{@link #get(String, int, int, RateLimitAlgorithm)} 对
     * {@code FIXED_WINDOW} / {@code TOKEN_BUCKET} / {@code SLIDING_WINDOW} / {@code LEAKY_BUCKET}
     * 返回 Redis 实现。
     *
     * @param defaultAlgorithm 默认算法
     * @param redisExecutor     Redis 命令执行器，可为 {@code null}（回落本地）
     * @param keys             Redis key 拼接工具，可为 {@code null}
     */
    public RateLimiterManager(RateLimitAlgorithm defaultAlgorithm,
                              @Nullable RedisCommandExecutor redisExecutor,
                              @Nullable RateLimitKeys keys) {
        this(defaultAlgorithm, redisExecutor, keys, true, null);
    }

    /**
     * 构造实例（全参数，供装配层使用）。
     * @param defaultAlgorithm 默认算法
     * @param redisExecutor     Redis 命令执行器，可为 {@code null}
     * @param keys             Redis key 拼接工具，可为 {@code null}
     * @param failOpen         Redis 异常时是否放行
     */
    public RateLimiterManager(RateLimitAlgorithm defaultAlgorithm,
                              @Nullable RedisCommandExecutor redisExecutor,
                              @Nullable RateLimitKeys keys,
                              boolean failOpen) {
        this(defaultAlgorithm, redisExecutor, keys, failOpen, null);
    }

    private RateLimiterManager(RateLimitAlgorithm defaultAlgorithm,
                               @Nullable RedisCommandExecutor redisExecutor,
                               @Nullable RateLimitKeys keys,
                               boolean failOpen,
                               @Nullable RateLimiterFactory factory) {
        this.defaultAlgorithm = defaultAlgorithm;
        this.redisExecutor = redisExecutor;
        this.keys = keys;
        this.failOpen = failOpen;
        this.factory = factory != null ? factory : this::createByAlgorithm;
    }

    /**
     * 获取（或创建）指定维度的限流器，使用默认算法。
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

    private RateLimiter createByAlgorithm(int permits, int window, RateLimitAlgorithm algorithm) {
        if (redisExecutor != null && redisExecutor.supportsScript() && keys != null) {
            return switch (algorithm) {
                case FIXED_WINDOW ->
                        new RedisFixedWindowRateLimiter(redisExecutor, keys, permits, window, failOpen);
                case TOKEN_BUCKET ->
                        new RedisTokenBucketRateLimiter(redisExecutor, keys, permits, window, failOpen);
                case SLIDING_WINDOW ->
                        new RedisSlidingWindowRateLimiter(redisExecutor, keys, permits, window, failOpen);
                case LEAKY_BUCKET ->
                        new RedisLeakyBucketRateLimiter(redisExecutor, keys, permits, window, failOpen);
            };
        }
        return localLimiter(permits, window, algorithm);
    }

    private static RateLimiter localLimiter(int permits, int window, RateLimitAlgorithm algorithm) {
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
