package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 滑动窗口限流器：统计窗口内的请求时间戳，窗口内请求数超过阈值则拒绝。
 *
 * <p>相比固定窗口，避免了窗口边界突刺，但内存占用随请求量增长。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class SlidingWindowRateLimiter implements RateLimiter {

    /** permits 不可变字段。 */
    private final int permits;
    /** windowMillis 不可变字段。 */
    private final long windowMillis;
    /** hits 不可变字段。 */
    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /**
     * 构造实例。
     * @param permits 参数 permits
     * @param windowSeconds 参数 windowSeconds
     */
    public SlidingWindowRateLimiter(int permits, int windowSeconds) {
        this.permits = permits;
        this.windowMillis = (long) windowSeconds * 1000L;
    }

    /**
     * 执行try acquire操作。
     * @param key 参数 key
     * @return 结果
     */
    @Override
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            long boundary = now - windowMillis;
            while (!deque.isEmpty() && deque.peekFirst() < boundary) {
                deque.pollFirst();
            }
            if (deque.size() < permits) {
                deque.addLast(now);
                return true;
            }
            return false;
        }
    }
}
