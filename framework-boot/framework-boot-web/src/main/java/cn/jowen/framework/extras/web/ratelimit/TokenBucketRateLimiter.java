package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的令牌桶限流器（单实例适用）。
 *
 * <p>多实例部署时应替换为 Redis 等分布式实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class TokenBucketRateLimiter implements RateLimiter {

    private final int permitsPerWindow;
    private final long windowMillis;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int permitsPerWindow, int windowSeconds) {
        this.permitsPerWindow = permitsPerWindow;
        this.windowMillis = (long) windowSeconds * 1000L;
    }

    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now, permitsPerWindow));
        return bucket.tryAcquire(now, permitsPerWindow, windowMillis);
    }

    private static final class Bucket {
        private final AtomicLong tokens;
        private volatile long windowStart;

        Bucket(long start, int capacity) {
            this.tokens = new AtomicLong(capacity);
            this.windowStart = start;
        }

        boolean tryAcquire(long now, int capacity, long windowMillis) {
            synchronized (this) {
                if (now - windowStart >= windowMillis) {
                    windowStart = now;
                    tokens.set(capacity);
                }
                if (tokens.get() > 0) {
                    tokens.decrementAndGet();
                    return true;
                }
                return false;
            }
        }
    }
}
