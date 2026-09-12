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
 * 基于 Redis 的漏桶限流器（多实例共享水位）。
 *
 * <p>通过 {@link RedisCommandExecutor#eval(String, List, List)} 执行原子「计算漏水量 + 判断水位 + 写入」脚本，
 * 跨实例共享同一桶状态。语义与本地 {@link LeakyBucketRateLimiter} 对齐：桶满则拒绝；
 * 水以恒定速率（{@code permits / windowSeconds} 每毫秒）漏出。属「最终一致」容忍范围——
 * 多实例间的水位同步存在 Redis 往返延迟，限流场景下毫秒级误差无实质影响。
 *
 * <p><b>构造器强制校验 {@code executor.supportsScript() == true}</b>：把「配了 Redis 却不支持脚本」
 * 的静默失效前移到创建期快速失败，避免运行期悄然回落本地、多实例计数不共享却难察觉。
 *
 * <p>Redis 运行期异常时按 {@code failOpen} 处置：默认放行；设为 {@code false} 时抛 {@link ExtrasException}。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public final class RedisLeakyBucketRateLimiter implements RateLimiter {

    /** 日志。 */
    private static final Logger log = LoggerFactory.getLogger(RedisLeakyBucketRateLimiter.class);

    /** executor 不可变字段。 */
    private final RedisCommandExecutor executor;

    /** keys 不可变字段。 */
    private final RateLimitKeys keys;

    /** capacity 不可变字段（= permits）。 */
    private final int capacity;

    /** leakPerSecond 不可变字段：每毫秒漏出的份额，换算成每秒便于脚本内除法。 */
    private final double leakPerSecond;

    /** failOpen 不可变字段。 */
    private final boolean failOpen;

    /**
     * 构造实例。
     * @param executor Redis 命令执行器（必须支持脚本），不可为 {@code null}
     * @param keys     Redis key 拼接工具，不可为 {@code null}
     * @param permits  桶容量（= 窗口内最大放行数），必须为正
     * @param windowSeconds 时间窗口（秒），决定漏水速率，必须为正
     * @param failOpen Redis 异常时是否放行
     */
    public RedisLeakyBucketRateLimiter(RedisCommandExecutor executor, RateLimitKeys keys,
                                       int permits, int windowSeconds, boolean failOpen) {
        Assert.notNull(executor, null, "executor must not be null");
        Assert.isTrue(executor.supportsScript(), null,
                "RedisCommandExecutor must support script execution for Redis rate limiter");
        Assert.notNull(keys, null, "keys must not be null");
        Assert.isTrue(permits > 0, null, "permits must be positive");
        Assert.isTrue(windowSeconds > 0, null, "windowSeconds must be positive");
        this.executor = executor;
        this.keys = keys;
        this.capacity = permits;
        // leakPerSecond = permits / windowSeconds，脚本内用 elapsed_seconds * leakPerSecond 计算漏水量
        this.leakPerSecond = (double) permits / windowSeconds;
        this.failOpen = failOpen;
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        String redisKey = keys.leakyBucket(key);
        long ttl = 3L * (long) Math.ceil(capacity / leakPerSecond);
        try {
            Object reply = executor.eval(RateLimitScripts.LEAKY_BUCKET,
                    List.of(redisKey),
                    List.of(Double.toString((double) now / 1000.0),
                            Double.toString(leakPerSecond),
                            Integer.toString(capacity)));
            return RedisScriptReplies.isAllowed(reply);
        } catch (RuntimeException ex) {
            if (failOpen) {
                log.warn("Redis leaky-bucket rate limiter backend unavailable, fail-open allowed: key={}, {}",
                        redisKey, ex.getMessage());
                return true;
            }
            throw new ExtrasException("Redis rate limiter backend unavailable: " + ex.getMessage(), ex);
        }
    }
}
