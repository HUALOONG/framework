package cn.jowen.framework.extras.lock;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;

/**
 * 分布式锁接口，扩展基础锁功能，支持强制释放和当前线程持有判断。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public interface DistributedLock extends Lock {

    /**
     * 判断锁是否已被任意线程持有。
     *
     * @return 已被持有返回 {@code true}
     */
    boolean isLocked();

    /**
     * 强制释放锁（忽略持有者）。
     *
     * @throws LockException 强制释放失败
     */
    void forceUnlock() throws LockException;

    /**
     * 判断当前线程是否持有该锁。
     *
     * @return 当前线程持有返回 {@code true}
     */
    boolean isHeldByCurrentThread();
}
