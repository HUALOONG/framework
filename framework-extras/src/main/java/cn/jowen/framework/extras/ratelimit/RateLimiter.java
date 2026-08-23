package cn.jowen.framework.extras.ratelimit;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;

/**
 * 限流器接口。各算法实现（固定窗口 / 滑动窗口 / 漏桶 / 令牌桶）统一暴露此接口。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface RateLimiter {

    /**
     * 尝试获取一个许可（非阻塞）。
     *
     * @return 获取成功返回 {@code true}
     */
    boolean tryAcquire();

    /**
     * 尝试获取 {@code permits} 个许可（非阻塞）。
     *
     * @param permits 许可数（>0）
     * @return 获取成功返回 {@code true}
     */
    boolean tryAcquire(long permits);

    /**
     * 在超时时间内尝试获取一个许可。
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return 获取成功返回 {@code true}，超时返回 {@code false}
     * @throws InterruptedException 等待被中断
     */
    boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * @return 当前可用许可数
     */
    long getAvailablePermits();

    /**
     * @return 算法名称
     */
    String algorithm();
}