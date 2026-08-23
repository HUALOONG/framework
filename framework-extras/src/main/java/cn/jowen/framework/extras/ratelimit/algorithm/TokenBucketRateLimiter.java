package cn.jowen.framework.extras.ratelimit.algorithm;

import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 令牌桶限流器。以恒定速率补充令牌，桶满为止；支持突发流量。
 *
 * <p>实现：每次获取前按已过时间补充令牌；令牌不足则拒绝（非阻塞）。
 * 与漏桶区别：允许短时突发（桶内有积攒令牌时快速消费）。
 * 线程安全，基于 {@link ReentrantLock}。
 */
@NullMarked
public final class TokenBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final long refillIntervalNanos;
    private long tokens;
    private long lastRefillNanos;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @param capacity 桶容量（>0）
     * @param refillIntervalNanos 每补充一个令牌的间隔（纳秒，>0）
     */
    public TokenBucketRateLimiter(long capacity, long refillIntervalNanos) {
        if (capacity <= 0 || refillIntervalNanos <= 0) {
            throw new IllegalArgumentException("令牌桶限流参数必须为正");
        }
        this.capacity = capacity;
        this.refillIntervalNanos = refillIntervalNanos;
        this.tokens = capacity;
        this.lastRefillNanos = System.nanoTime();
    }

    @Override
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    @Override
    public boolean tryAcquire(long permits) {
        long now = System.nanoTime();
        lock.lock();
        try {
            refill(now);
            if (tokens < permits) {
                return false;
            }
            tokens -= permits;
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (System.nanoTime() <= deadline) {
            if (tryAcquire()) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    @Override
    public long getAvailablePermits() {
        lock.lock();
        try {
            refill(System.nanoTime());
            return tokens;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String algorithm() {
        return "token-bucket";
    }

    private void refill(long now) {
        long elapsed = now - lastRefillNanos;
        long added = elapsed / refillIntervalNanos;
        if (added > 0) {
            tokens = Math.min(capacity, tokens + added);
            lastRefillNanos += added * refillIntervalNanos;
        }
    }
}