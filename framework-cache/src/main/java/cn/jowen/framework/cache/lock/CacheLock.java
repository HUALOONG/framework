package cn.jowen.framework.cache.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;

/**
 * 缓存锁接口。用于防止缓存击穿，确保同一时刻只有一个线程回源查询。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface CacheLock {

    /**
     * 尝试获取锁。
     *
     * @param waitTime  最大等待时间
     * @param leaseTime 锁持有时间；{@code null} 表示不自动释放
     * @return {@code true} 表示获取成功
     */
    boolean tryLock(Duration waitTime, @Nullable Duration leaseTime);

    /**
     * 释放锁。
     */
    void unlock();

    /**
     * @return 当前是否持有锁
     */
    boolean isLocked();
}
