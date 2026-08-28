package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;

/**
 * 分布式锁抽象。多实例部署时由调用方注入基于 Redis 等的实现。
 *
 * <p>框架仅提供契约，不强制依赖任何分布式组件；未提供实现时 {@link LockType#REDIS}
 * 会降级为本地锁并给出告警日志。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface DistributedLock {

    /**
     * 获取分布式锁。
     *
     * @param key         锁键
     * @param waitMillis  最大等待毫秒数（<=0 表示不等待）
     * @param leaseMillis 锁租约毫秒数，超时自动释放
     * @return 锁对象，未获取到抛出 {@link LockAcquireException}
     */
    Lock acquire(String key, long waitMillis, long leaseMillis);
}
