package cn.jowen.framework.extras.lock;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 进程内锁实现。基于 {@link ReentrantLock}，零外部依赖，适用于单实例场景。
 * 分布式场景请扩展实现（如基于 Redis 的 {@code SET NX}）。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LocalLock implements Lock {

    private final ReentrantLock delegate = new ReentrantLock();

    @Override
    public boolean tryLock() {
        return delegate.tryLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return delegate.tryLock(time, unit);
    }

    @Override
    public void unlock() {
        if (delegate.isHeldByCurrentThread()) {
            delegate.unlock();
        }
    }
}
