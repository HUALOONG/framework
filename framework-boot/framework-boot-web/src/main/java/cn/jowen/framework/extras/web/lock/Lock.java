package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;

import java.io.Closeable;

/**
 * 可释放锁抽象。实现应保证 {@link #close()} 能释放锁（配合 try-with-resources 使用）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public interface Lock extends Closeable {

    /**
     * 尝试获取锁，获取失败抛出 {@link LockAcquireException}。
     *
     * @throws LockAcquireException 获取锁失败时
     */
    void lock();

    /**
     * 释放锁。
     */
    @Override
    void close();
}
