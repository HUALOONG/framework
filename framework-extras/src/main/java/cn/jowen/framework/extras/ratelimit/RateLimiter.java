package cn.jowen.framework.extras.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 令牌桶限流器（纯算法，零外部依赖）。线程安全，支持突发流量平滑。
 *
 * <p>算法：以 {@code permitsPerSecond} 恒定速率补充令牌，桶容量 {@code capacity} 限制突发；
 * 每次获取扣除令牌，不足时返回 {@code false}（非阻塞）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class RateLimiter {

    private final long capacity;
    private final double permitsPerSecond;
    private final AtomicLong storedPermits;
    private final AtomicLong lastRefillNanos;
    private final double refillIntervalNanos;

    /**
     * 构造限流器。
     *
     * @param permitsPerSecond 每秒许可数（>0）
     * @param capacity         桶容量（>0）
     */
    public RateLimiter(double permitsPerSecond, long capacity) {
        if (permitsPerSecond <= 0 || capacity <= 0) {
            throw new IllegalArgumentException("限流参数必须为正");
        }
        this.permitsPerSecond = permitsPerSecond;
        this.capacity = capacity;
        this.storedPermits = new AtomicLong(capacity);
        this.lastRefillNanos = new AtomicLong(nowNanos());
        this.refillIntervalNanos = 1_000_000_000.0 / permitsPerSecond;
    }

    /**
     * 尝试获取一个许可（非阻塞）。
     *
     * @return 获取成功返回 {@code true}
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * 尝试获取 {@code permits} 个许可（非阻塞）。
     *
     * @param permits 许可数（>0）
     * @return 获取成功返回 {@code true}
     */
    public boolean tryAcquire(long permits) {
        if (permits <= 0 || permits > capacity) {
            return false;
        }
        refill();
        long current = storedPermits.get();
        if (current >= permits) {
            return storedPermits.compareAndSet(current, current - permits);
        }
        return false;
    }

    private void refill() {
        long now = nowNanos();
        long last = lastRefillNanos.get();
        long elapsed = now - last;
        if (elapsed < refillIntervalNanos) {
            return;
        }
        long added = (long) (elapsed / refillIntervalNanos);
        if (added <= 0) {
            return;
        }
        // CAS 更新时间戳与令牌，避免多线程重复补充
        if (lastRefillNanos.compareAndSet(last, last + (long) (added * refillIntervalNanos))) {
            storedPermits.accumulateAndGet(added, (cur, add) -> Math.min(capacity, cur + add));
        }
    }

    private static long nowNanos() {
        return System.nanoTime();
    }
}
