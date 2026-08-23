package cn.jowen.framework.extras.ratelimit.algorithm;

import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 固定窗口限流器。窗口内计数，窗口结束重置。
 *
 * <p>实现：以 {@code periodMillis} 为窗口边界，窗口起始时刻为当前计数开始时间；
 * 超过窗口后计数归零并重建窗口。线程安全，基于 {@link AtomicLong}。
 */
@NullMarked
public final class FixedWindowRateLimiter implements RateLimiter {

    private final long permits;
    private final long periodNanos;
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong windowStart = new AtomicLong(System.nanoTime());

    /**
     * @param permits 每个窗口内最大许可数（>0）
     * @param periodNanos 窗口周期（纳秒，>0）
     */
    public FixedWindowRateLimiter(long permits, long periodNanos) {
        if (permits <= 0 || periodNanos <= 0) {
            throw new IllegalArgumentException("固定窗口限流参数必须为正");
        }
        this.permits = permits;
        this.periodNanos = periodNanos;
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(long permits) {
        long now = System.nanoTime();
        long start = windowStart.get();
        if (now - start >= periodNanos) {
            if (windowStart.compareAndSet(start, now)) {
                count.set(0);
            } else {
                return tryAcquire(permits);
            }
        }
        long current = count.get();
        if (current + permits > this.permits) {
            return false;
        }
        return count.addAndGet(permits) <= this.permits + permits;
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        if (tryAcquire()) {
            return true;
        }
        long nanos = unit.toNanos(timeout);
        while (nanos > 0) {
            Thread.sleep(Math.min(10, nanos / 1_000_000));
            nanos = unit.toNanos(timeout) - (System.nanoTime() - System.nanoTime());
            if (tryAcquire()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public long getAvailablePermits() {
        long now = System.nanoTime();
        long start = windowStart.get();
        if (now - start >= periodNanos) {
            return permits;
        }
        return Math.max(0, permits - count.get());
    }

    @Override
    public String algorithm() {
        return "fixed-window";
    }
}