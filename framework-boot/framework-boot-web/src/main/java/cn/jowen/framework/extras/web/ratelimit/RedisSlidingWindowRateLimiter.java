package cn.jowen.framework.extras.web.ratelimit;

import cn.jowen.framework.core.assertion.Assert;
import cn.jowen.framework.extras.common.exception.ExtrasException;
import cn.jowen.framework.extras.web.lock.RedisCommandExecutor;
import cn.jowen.framework.extras.web.lock.RedisScriptReplies;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 基于 Redis 的滑动窗口限流器（多实例共享请求时间戳）。
 *
 * <p>通过 {@link RedisCommandExecutor#eval(String, List, List)} 执行原子 ZADD + ZREMRANGEBYSCORE
 * + ZCARD 脚本，跨实例共享同一窗口内的请求时间戳集合。语义与本地
 * {@link SlidingWindowRateLimiter} 对齐：窗口内超过 {@code permits} 次的请求被拒绝；
 * 过期时间戳在每次请求时惰性清除。属「最终一致」容忍范围——毫秒级误差对限流无实质影响。
 *
 * <p><b>构造器强制校验 {@code executor.supportsScript() == true}</b>：把「配了 Redis 却不支持脚本」
 * 的静默失效前移到创建期快速失败，避免运行期悄然回落本地、多实例计数不共享却难察觉。
 *
 * <p>Redis 运行期异常时按 {@code failOpen} 处置：默认放行（限流非安全边界，避免 Redis 抖动导致全站 503）；
 * 设为 {@code false} 时抛 {@link ExtrasException}（金融 / 秒杀 / 防刷等硬配额场景）。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public final class RedisSlidingWindowRateLimiter implements RateLimiter {

    /** 日志。 */
    private static final Logger log = LoggerFactory.getLogger(RedisSlidingWindowRateLimiter.class);

    /** executor 不可变字段。 */
    private final RedisCommandExecutor executor;

    /** keys 不可变字段。 */
    private final RateLimitKeys keys;

    /** permits 不可变字段。 */
    private final int permits;

    /** windowMillis 不可变字段。 */
    private final long windowMillis;

    /** failOpen 不可变字段。 */
    private final boolean failOpen;

    /**
     * 构造实例。
     * @param executor Redis 命令执行器（必须支持脚本），不可为 {@code null}
     * @param keys     Redis key 拼接工具，不可为 {@code null}
     * @param permits  窗口内最大放行数，必须为正
     * @param windowSeconds 时间窗口（秒），必须为正
     * @param failOpen Redis 异常时是否放行
     */
    public RedisSlidingWindowRateLimiter(RedisCommandExecutor executor, RateLimitKeys keys,
                                         int permits, int windowSeconds, boolean failOpen) {
        Assert.notNull(executor, null, "executor must not be null");
        Assert.isTrue(executor.supportsScript(), null,
                "RedisCommandExecutor must support script execution for Redis rate limiter");
        Assert.notNull(keys, null, "keys must not be null");
        Assert.isTrue(permits > 0, null, "permits must be positive");
        Assert.isTrue(windowSeconds > 0, null, "windowSeconds must be positive");
        this.executor = executor;
        this.keys = keys;
        this.permits = permits;
        this.windowMillis = (long) windowSeconds * 1000L;
        this.failOpen = failOpen;
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        String redisKey = keys.slidingWindow(key);
        long ttl = 2L * windowMillis;
        try {
            Object reply = executor.eval(RateLimitScripts.SLIDING_WINDOW,
                    List.of(redisKey),
                    List.of(Double.toString((double) now / 1000.0),
                            Double.toString(windowMillis / 1000.0),
                            Integer.toString(permits),
                            Long.toString(ttl)));
            return RedisScriptReplies.isAllowed(reply);
        } catch (RuntimeException ex) {
            if (failOpen) {
                log.warn("Redis sliding-window rate limiter backend unavailable, fail-open allowed: key={}, {}",
                        redisKey, ex.getMessage());
                return true;
            }
            throw new ExtrasException("Redis rate limiter backend unavailable: " + ex.getMessage(), ex);
        }
    }
}
