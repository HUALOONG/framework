package cn.jowen.framework.plugin.hotswap;

import org.jspecify.annotations.NullMarked;

/**
 * 防抖定时器，确保短时间内多次触发只执行最后一次。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class DebounceTimer {

    private final long delayMs;
    private final java.util.concurrent.ScheduledExecutorService scheduler =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "debounce-timer");
                t.setDaemon(true);
                return t;
            });
    private final java.util.concurrent.atomic.AtomicBoolean pending = new java.util.concurrent.atomic.AtomicBoolean(false);

    public DebounceTimer(long delayMs) {
        this.delayMs = delayMs;
    }

    public void runOnce(Runnable runnable) {
        pending.set(true);
        scheduler.schedule(() -> {
            if (pending.compareAndSet(true, false) && runnable != null) {
                runnable.run();
            }
        }, delayMs, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
