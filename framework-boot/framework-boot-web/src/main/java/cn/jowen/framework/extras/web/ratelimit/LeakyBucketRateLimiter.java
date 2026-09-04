package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 漏桶限流器：请求入桶，桶以恒定速率漏水（放行），桶满则拒绝。
 *
 * <p>输出速率恒定，适合削峰填谷。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LeakyBucketRateLimiter implements RateLimiter {

    /** capacity 不可变字段。 */
    private final int capacity;
    /** leakPerMillis 不可变字段；非正窗口时为 {@link Double#MAX_VALUE}，表示「不限流」。 */
    private final double leakPerMillis;
    /** buckets 不可变字段。 */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * 构造实例。
     * @param permits 参数 permits
     * @param windowSeconds 参数 windowSeconds
     */
    public LeakyBucketRateLimiter(int permits, int windowSeconds) {
        this.capacity = permits;
        this.leakPerMillis = windowSeconds <= 0 ? Double.MAX_VALUE : (double) permits / (windowSeconds * 1000L);
    }

    /**
     * 执行try acquire操作。
     * @param key 参数 key
     * @return 结果
     */
    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now));
        return bucket.tryAcquire(now, capacity, leakPerMillis);
    }

    /**
     * 「Bucket」封装相关能力。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    private static final class Bucket {
        /** water 字段。 */
        private double water;
        /** lastLeak 字段。 */
        private long lastLeak;

        Bucket(long now) {
            this.water = 0;
            this.lastLeak = now;
        }

        synchronized boolean tryAcquire(long now, int capacity, double leakPerMillis) {
            // 非正窗口（leakPerMillis 取无穷大）表示「不限流」，直接放行，等价于桶立即彻底漏空。
            if (leakPerMillis == Double.MAX_VALUE) {
                return true;
            }
            long elapsed = now - lastLeak;
            // 防御：时钟回拨（elapsed<0）或同毫秒重复调用（elapsed==0）时不执行减法，
            // 避免 0*Infinity=NaN 或负时间导致 water 被污染后永久拒流。
            if (elapsed > 0) {
                water = Math.max(0, water - elapsed * leakPerMillis);
                lastLeak = now;
            }
            if (water + 1 <= capacity) {
                water += 1;
                return true;
            }
            return false;
        }
    }
}
