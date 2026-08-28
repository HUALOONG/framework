package cn.jowen.framework.extras.web.idempotent;

import org.jspecify.annotations.NullMarked;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于内存的幂等指纹存储（单实例适用，重启即失效）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class LocalIdempotentStore implements IdempotentStore {

    private final ConcurrentHashMap<String, AtomicLong> store = new ConcurrentHashMap<>();

    @Override
    public boolean tryMark(String key, long expire, TimeUnit unit) {
        long expireAt = System.currentTimeMillis() + unit.toMillis(expire);
        AtomicLong prev = store.putIfAbsent(key, new AtomicLong(expireAt));
        if (prev == null) {
            return true;
        }
        if (prev.get() < System.currentTimeMillis()) {
            return store.replace(key, prev, new AtomicLong(expireAt));
        }
        return false;
    }

    @Override
    public void remove(String key) {
        store.remove(key);
    }
}
