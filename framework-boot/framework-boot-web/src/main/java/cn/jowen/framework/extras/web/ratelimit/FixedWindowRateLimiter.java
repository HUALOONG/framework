package cn.jowen.framework.extras.web.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 固定窗口限流器：每个时间窗口内最多放行 {@code permits} 次，窗口切换时计数清零。
 *
 * <p>实现简单，但在窗口边界可能出现 2 倍突刺。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class FixedWindowRateLimiter implements RateLimiter {

    /** permits 不可变字段。 */
    private final int permits;
    /** windowMillis 不可变字段。 */
    private final long windowMillis;
    /** windows 不可变字段。 */
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * 构造实例。
     * @param permits 参数 permits
     * @param windowSeconds 参数 windowSeconds
     */
    public FixedWindowRateLimiter(int permits, int windowSeconds) {
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
        Window window = windows.computeIfAbsent(key, k -> new Window(now / windowMillis));
        return window.tryAcquire(now / windowMillis, permits);
    }

    /**
     * 「Window」封装相关能力。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    private static final class Window {
        /** count 不可变字段。 */
        private final AtomicInteger count = new AtomicInteger(0);
        /** index 字段。 */
        private volatile long index;

        Window(long index) {
            this.index = index;
        }

        boolean tryAcquire(long currentIndex, int permits) {
            if (currentIndex != index) {
                synchronized (this) {
                    if (currentIndex != index) {
                        index = currentIndex;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet() <= permits;
        }
    }
}
