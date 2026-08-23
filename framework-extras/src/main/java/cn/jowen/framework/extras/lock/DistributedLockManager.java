package cn.jowen.framework.extras.lock;

import cn.jowen.framework.cache.api.CacheManager;
import cn.jowen.framework.extras.config.LockProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁管理器，提供锁的创建和获取入口。
 *
 * <p>支持两种模式：
 * <ul>
 *   <li>本地模式（默认）：仅构造 {@link LocalDistributedLock}，零外部依赖</li>
 *   <li>Redis 模式：注入 {@link CacheManager} + {@link LockProperties} 后，按配置构造 {@link RedisDistributedLock}</li>
 * </ul>
 */
@NullMarked
public final class DistributedLockManager {

    private final Map<String, DistributedLock> _lockCache = new ConcurrentHashMap<>();
    private final @Nullable CacheManager _cacheManager;
    private final @Nullable LockProperties _properties;

    public DistributedLockManager() {
        this(null, null);
    }

    public DistributedLockManager(CacheManager cacheManager, @Nullable LockProperties properties) {
        this._cacheManager = cacheManager;
        this._properties = properties;
    }

    /**
     * 获取可重入锁。
     */
    public DistributedLock getLock(String lockName) {
        return getOrCreateLock(lockName, LockType.REENTRANT);
    }

    /**
     * 获取公平锁。
     */
    public DistributedLock getFairLock(String lockName) {
        return getOrCreateLock(lockName, LockType.FAIR);
    }

    /**
     * 获取读锁。
     */
    public DistributedLock getReadLock(String lockName) {
        return getOrCreateLock(lockName, LockType.READ);
    }

    /**
     * 获取写锁。
     */
    public DistributedLock getWriteLock(String lockName) {
        return getOrCreateLock(lockName, LockType.WRITE);
    }

    /**
     * 获取联锁（多个锁同时持有）。
     */
    public DistributedLock getMultiLock(String... lockNames) {
        return new MultiLock(lockNames);
    }

    /**
     * 获取红锁（等价于联锁，语义上用于多 Redis 节点场景）。
     */
    public DistributedLock getRedLock(String... lockNames) {
        return new MultiLock(lockNames);
    }

    private DistributedLock getOrCreateLock(String lockName, LockType type) {
        if (isRedisMode()) {
            return _lockCache.computeIfAbsent(lockName,
                    k -> new RedisDistributedLock(_cacheManager,
                            _properties.getKeyPrefix(),
                            k,
                            _properties.getDefaultLeaseTime(),
                            _properties.isWatchdogEnabled()));
        }
        return _lockCache.computeIfAbsent(lockName, k -> new LocalDistributedLock(k, type));
    }

    private boolean isRedisMode() {
        return _cacheManager != null && _properties != null;
    }

    /**
     * 返回已缓存的锁数量。
     */
    public int size() {
        return _lockCache.size();
    }

    /**
     * 清除全部缓存锁。
     */
    public void clear() {
        _lockCache.clear();
    }

    /**
     * 联锁实现，同时持有多个锁。
     */
    @NullMarked
    public final class MultiLock implements DistributedLock {

        private final String[] lockNames;
        private final DistributedLock[] locks;

        public MultiLock(String[] lockNames) {
            this.lockNames = lockNames;
            this.locks = new DistributedLock[lockNames.length];
            for (int i = 0; i < lockNames.length; i++) {
                locks[i] = getLock(lockNames[i]);
            }
        }

        @Override
        public boolean tryLock() {
            for (DistributedLock lock : locks) {
                if (!lock.tryLock()) {
                    rollbackBefore(lockNames.length);
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
            long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
            for (int i = 0; i < locks.length; i++) {
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    rollbackBefore(i);
                    return false;
                }
                if (!locks[i].tryLock(remaining, TimeUnit.MILLISECONDS)) {
                    rollbackBefore(i);
                    return false;
                }
            }
            return true;
        }

        @Override
        public void unlock() {
            for (DistributedLock lock : locks) {
                lock.unlock();
            }
        }

        @Override
        public void forceUnlock() throws LockException {
            for (DistributedLock lock : locks) {
                lock.forceUnlock();
            }
        }

        @Override
        public boolean isLocked() {
            for (DistributedLock lock : locks) {
                if (!lock.isLocked()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean isHeldByCurrentThread() {
            for (DistributedLock lock : locks) {
                if (!lock.isHeldByCurrentThread()) {
                    return false;
                }
            }
            return true;
        }

        private void rollbackBefore(int until) {
            for (int i = 0; i < until; i++) {
                locks[i].unlock();
            }
        }

        public String[] getLockNames() {
            return lockNames.clone();
        }
    }
}