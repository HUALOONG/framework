package cn.jowen.framework.extras.lock;

import cn.jowen.framework.cache.api.Cache;
import cn.jowen.framework.cache.api.CacheManager;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁实现。
 *
 * <p>基于 {@link CacheManager} 实现（支持 Caffeine / Redisson / 多级缓存），
 * 使用 {@code putIfAbsent} 原子获取锁，通过 {@code clientId:uuid} 唯一值判断持有者。
 * 支持可重入（同 clientId 多次获取）、Watchdog 自动续期。
 *
 * <p>锁 Key：{@code <prefix><lockKey>}，值：{@code <clientId>:<uuid>}。
 */
@NullMarked
public class RedisDistributedLock implements DistributedLock {

    private static final String LOCK_CACHE_NAME = "lock";
    private static final String KEY_SEPARATOR = ":";

    private final CacheManager _cacheManager;
    private final String _lockKey;
    private final String _clientId;
    private final long _leaseTimeMillis;
    private final boolean _watchdogEnabled;
    private int _holdCount;
    private @Nullable String _heldValue;
    private @Nullable ScheduledExecutorService _watchdogExecutor;

    public RedisDistributedLock(CacheManager cacheManager, String keyPrefix,
                                 String lockKey, long leaseTimeMillis, boolean watchdogEnabled) {
        this._cacheManager = cacheManager;
        this._lockKey = keyPrefix + lockKey;
        this._clientId = UUID.randomUUID().toString();
        this._leaseTimeMillis = leaseTimeMillis;
        this._watchdogEnabled = watchdogEnabled;
    }

    @Override
    public boolean tryLock() {
        return doTryLock();
    }

    @Override
    public boolean tryLock(long waitTime, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(waitTime);
        while (System.currentTimeMillis() < deadline) {
            if (doTryLock()) {
                return true;
            }
            Thread.sleep(Math.min(50, deadline - System.currentTimeMillis()));
        }
        return false;
    }

    @Override
    public void unlock() {
        if (_holdCount <= 0) {
            return;
        }
        _holdCount--;
        if (_holdCount > 0) {
            return;
        }
        stopWatchdog();
        String heldValue = _heldValue;
        if (heldValue != null) {
            @SuppressWarnings("unchecked")
            Cache<String, String> cache = _cacheManager.<String, String>getCache(LOCK_CACHE_NAME);
            String current = cache.get(_lockKey);
            if (heldValue.equals(current)) {
                cache.evict(_lockKey);
            }
        }
        _heldValue = null;
    }

    @Override
    public boolean isLocked() {
        @SuppressWarnings("unchecked")
        Cache<String, String> cache = _cacheManager.<String, String>getCache(LOCK_CACHE_NAME);
        return cache.get(_lockKey) != null;
    }

    @Override
    public boolean isHeldByCurrentThread() {
        return _holdCount > 0;
    }

    @Override
    public void forceUnlock() throws LockException {
        stopWatchdog();
        try {
            @SuppressWarnings("unchecked")
            Cache<String, String> cache = _cacheManager.<String, String>getCache(LOCK_CACHE_NAME);
            cache.evict(_lockKey);
        } catch (Exception e) {
            throw new LockException("强制释放锁失败: " + _lockKey, e);
        }
        _holdCount = 0;
        _heldValue = null;
    }

    private boolean doTryLock() {
        if (_holdCount > 0) {
            _holdCount++;
            return true;
        }
        String value = _clientId + KEY_SEPARATOR + UUID.randomUUID();
        @SuppressWarnings("unchecked")
        Cache<String, String> cache = _cacheManager.<String, String>getCache(LOCK_CACHE_NAME);
        Boolean result = cache.putIfAbsent(_lockKey, value);
        if (Boolean.TRUE.equals(result)) {
            _holdCount = 1;
            _heldValue = value;
            setTtl(value);
            if (_watchdogEnabled) {
                startWatchdog();
            }
            return true;
        }
        String existing = cache.get(_lockKey);
        if (existing != null && existing.startsWith(_clientId + KEY_SEPARATOR)) {
            _holdCount = 1;
            _heldValue = value;
            setTtl(value);
            return true;
        }
        return false;
    }

    private void setTtl(String value) {
        @SuppressWarnings("unchecked")
        Cache<String, String> cache = _cacheManager.<String, String>getCache(LOCK_CACHE_NAME);
        Duration ttl = Duration.ofMillis(_leaseTimeMillis);
        cache.put(_lockKey, value, ttl);
    }

    private void startWatchdog() {
        _watchdogExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "lock-watchdog-" + _lockKey);
            t.setDaemon(true);
            return t;
        });
        long renewalInterval = Math.max(1000, _leaseTimeMillis / 3);
        _watchdogExecutor.scheduleAtFixedRate(this::renew, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
    }

    private void renew() {
        if (_holdCount <= 0) {
            return;
        }
        String heldValue = _heldValue;
        if (heldValue == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        Cache<String, String> cache = _cacheManager.<String, String>getCache(LOCK_CACHE_NAME);
        String current = cache.get(_lockKey);
        if (heldValue.equals(current)) {
            Duration ttl = Duration.ofMillis(_leaseTimeMillis);
            cache.put(_lockKey, heldValue, ttl);
        }
    }

    private void stopWatchdog() {
        if (_watchdogExecutor != null) {
            _watchdogExecutor.shutdownNow();
            _watchdogExecutor = null;
        }
    }

    public String getLockKey() {
        return _lockKey;
    }

    public int getHoldCount() {
        return _holdCount;
    }
}