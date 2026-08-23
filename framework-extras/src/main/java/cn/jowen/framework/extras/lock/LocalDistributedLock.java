package cn.jowen.framework.extras.lock;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 本地分布式锁实现（单机兜底）。
 *
 * <p>按 {@link LockType} 选择底层原语：
 * <ul>
 *   <li>REENTRANT / FAIR → {@link ReentrantLock}（FAIR 模式 {@code fair=true}）</li>
 *   <li>READ / WRITE → {@link ReentrantReadWriteLock}</li>
 * </ul>
 */
@NullMarked
public final class LocalDistributedLock implements DistributedLock {

    private final String lockName;
    private final LockType type;
    private final ReentrantLock _reentrantLock;
    private final ReentrantReadWriteLock _readWriteLock;

    public LocalDistributedLock(String lockName, LockType type) {
        this.lockName = lockName;
        this.type = type;
        this._reentrantLock = new ReentrantLock(type == LockType.FAIR);
        this._readWriteLock = new ReentrantReadWriteLock(type == LockType.FAIR);
    }

    @Override
    public boolean tryLock() {
        return selectLock().tryLock();
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        return acquire(() -> selectLock().tryLock(waitTime, unit), null);
    }

    @Override
    public void unlock() {
        if (isHeldByCurrentThread()) {
            selectLock().unlock();
        }
    }

    @Override
    public void forceUnlock() throws LockException {
        while (isLocked()) {
            selectLock().unlock();
        }
    }

    @Override
    public boolean isLocked() {
        return switch (type) {
            case READ -> _readWriteLock.getReadLockCount() > 0;
            case WRITE -> _readWriteLock.isWriteLocked();
            default -> _reentrantLock.isLocked();
        };
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return switch (type) {
            case READ -> _readWriteLock.getReadLockCount() > 0;
            case WRITE -> _readWriteLock.isWriteLockedByCurrentThread();
            default -> _reentrantLock.isHeldByCurrentThread();
        };
    }

    private java.util.concurrent.locks.Lock selectLock() {
        if (type == LockType.READ) {
            return _readWriteLock.readLock();
        }
        if (type == LockType.WRITE) {
            return _readWriteLock.writeLock();
        }
        return _reentrantLock;
    }

    @FunctionalInterface
    private interface LockTryAcquireAction {
        boolean execute() throws InterruptedException;
    }

    private boolean acquire(LockTryAcquireAction action, InterruptedException saved) throws InterruptedException {
        try {
            return action.execute();
        } catch (InterruptedException e) {
            if (saved != null) {
                e.addSuppressed(saved);
            }
            Thread.currentThread().interrupt();
            throw e;
        }
    }

    public String getLockName() {
        return lockName;
    }

    public LockType getType() {
        return type;
    }
}