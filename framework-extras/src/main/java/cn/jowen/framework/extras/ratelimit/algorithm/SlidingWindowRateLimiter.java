package cn.jowen.framework.extras.ratelimit.algorithm;

import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.jspecify.annotations.NullMarked;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 滑动窗口限流器。以时间戳集合精确统计窗口内请求数，避免固定窗口的突刺问题。
 *
 * <p>实现：维护一条有序的请求时间戳队列，每次请求前清理过期时间戳；
 * 队列长度达到 {@code permits} 时拒绝。线程安全，基于 {@link ReentrantLock} 保证清理与判定的原子性。
 */
@NullMarked
public final class SlidingWindowRateLimiter implements RateLimiter {

    private final long permits;
    private final long periodNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @param permits 窗口内最大许可数（>0）
     * @param periodNanos 窗口周期（纳秒，>0）
     */
    public SlidingWindowRateLimiter(long permits, long periodNanos) {
        if (permits <= 0 || periodNanos <= 0) {
            throw new IllegalArgumentException("滑动窗口限流参数必须为正");
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
        lock.lock();
        try {
            evictExpired(now);
            if (timestamps.size() + permits > this.permits) {
                return false;
            }
            for (int i = 0; i < permits; i++) {
                timestamps.addLast(now);
            }
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
            evictExpired(System.nanoTime());
            return Math.max(0, permits - timestamps.size());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String algorithm() {
        return "sliding-window";
    }

    private void evictExpired(long now) {
        long cutoff = now - periodNanos;
        while (!timestamps.isEmpty() && timestamps.peekFirst() <= cutoff) {
            timestamps.pollFirst();
        }
    }
}