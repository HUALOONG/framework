package cn.jowen.framework.extras.ratelimit.algorithm;

import cn.jowen.framework.extras.ratelimit.RateLimiter;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 漏桶限流器。以固定速率流出请求，超出部分丢弃。
 *
 * <p>实现：维护桶容量与下次可流出时间戳；请求到达时若桶未满则入桶，否则拒绝；
 * 流出口按恒定速率消费。线程安全，基于 {@link ReentrantLock}。
 */
@NullMarked
public final class LeakyBucketRateLimiter implements RateLimiter {

    private final long capacity;
    private final long drainIntervalNanos;
    private long bucketSize;
    private long lastDrainNanos;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * @param capacity 桶容量（>0）
     * @param drainIntervalNanos 每次流出一个请求的间隔（纳秒，>0）
     */
    public LeakyBucketRateLimiter(long capacity, long drainIntervalNanos) {
        if (capacity <= 0 || drainIntervalNanos <= 0) {
            throw new IllegalArgumentException("漏桶限流参数必须为正");
        }
        this.capacity = capacity;
        this.drainIntervalNanos = drainIntervalNanos;
        this.lastDrainNanos = System.nanoTime();
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
            drain(now);
            if (bucketSize + permits > capacity) {
                return false;
            }
            bucketSize += permits;
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
            drain(System.nanoTime());
            return Math.max(0, capacity - bucketSize);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String algorithm() {
        return "leaky-bucket";
    }

    private void drain(long now) {
        long elapsed = now - lastDrainNanos;
        long drained = elapsed / drainIntervalNanos;
        if (drained > 0) {
            bucketSize = Math.max(0, bucketSize - drained);
            lastDrainNanos += drained * drainIntervalNanos;
        }
    }
}