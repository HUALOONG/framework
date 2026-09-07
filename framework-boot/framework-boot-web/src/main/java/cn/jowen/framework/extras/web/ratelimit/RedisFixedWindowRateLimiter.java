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
 * 基于 Redis 的固定窗口限流器（多实例共享计数）。
 *
 * <p>通过 {@link RedisCommandExecutor#eval(String, List, List)} 执行原子 INCR + PEXPIRE 脚本，
 * 跨实例共享同一窗口计数。语义与本地 {@link FixedWindowRateLimiter} 对齐：窗口边界可能 2×突刺，
 * 属「最终一致」容忍范围（详见限流集群化设计文档）。
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
public final class RedisFixedWindowRateLimiter implements RateLimiter {

    /** 日志。 */
    private static final Logger log = LoggerFactory.getLogger(RedisFixedWindowRateLimiter.class);

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
    public RedisFixedWindowRateLimiter(RedisCommandExecutor executor, RateLimitKeys keys,
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
        long windowIndex = now / windowMillis;
        String redisKey = keys.fixedWindow(key, windowIndex);
        long ttl = 2L * windowMillis;
        try {
            Object reply = executor.eval(RateLimitScripts.FIXED_WINDOW,
                    List.of(redisKey),
                    List.of(Integer.toString(permits), Long.toString(ttl)));
            return RedisScriptReplies.isAllowed(reply);
        } catch (RuntimeException ex) {
            if (failOpen) {
                log.warn("Redis fixed-window rate limiter backend unavailable, fail-open allowed: key={}, {}",
                        redisKey, ex.getMessage());
                return true;
            }
            throw new ExtrasException("Redis rate limiter backend unavailable: " + ex.getMessage(), ex);
        }
    }
}
