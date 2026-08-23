package cn.jowen.framework.extras.lock;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;

/**
 * 锁抽象。与具体实现（本地/Redis/ZK）解耦，仅依赖 JDK 并发原语语义。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public interface Lock {

    /**
     * 尝试获取锁（非阻塞）。
     *
     * @return 获取成功返回 {@code true}
     */
    boolean tryLock();

    /**
     * 尝试在超时内获取锁。
     *
     * @param time 超时时间
     * @param unit 时间单位
     * @return 获取成功返回 {@code true}
     * @throws InterruptedException 等待被中断
     */
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException;

    /**
     * 释放锁。
     */
    void unlock();
}
