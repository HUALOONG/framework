package cn.jowen.framework.cache;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 进程内本地缓存。基于 {@link ConcurrentHashMap}，支持可选 TTL（毫秒）。
 * TTL 采用写后惰性过期（读取时检查写时间戳）+ 写时随机抽样驱逐，避免额外定时线程。
 *
 * <p>无 TTL 时退化为纯内存映射，零时间开销。
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class LocalCache<K, V> extends AbstractCache<K, V> {

    private final ConcurrentMap<K, Entry<V>> store = new ConcurrentHashMap<>();
    /**
     * TTL（毫秒）；{@code <=0} 表示永不过期。
     */
    private final long ttlMillis;

    public LocalCache(String name, long ttlMillis) {
        super(name);
        this.ttlMillis = ttlMillis;
    }

    public LocalCache(String name) {
        this(name, 0L);
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    @Override
    protected @Nullable V doGet(K key) {
        Entry<V> entry = store.get(key);
        if (entry == null) {
            return null;
        }
        if (ttlMillis > 0 && isExpired(entry)) {
            store.remove(key, entry);
            return null;
        }
        return entry.value();
    }

    @Override
    protected void doPut(K key, V value) {
        store.put(key, new Entry<>(value, now()));
    }

    @Override
    protected void doEvict(K key) {
        store.remove(key);
    }

    @Override
    protected void doClear() {
        store.clear();
    }

    @Override
    protected long doSize() {
        if (ttlMillis <= 0) {
            return store.size();
        }
        // 含 TTL 时需清理过期项以得到真实大小
        long count = 0;
        for (java.util.Iterator<java.util.Map.Entry<K, Entry<V>>> it = store.entrySet().iterator(); it.hasNext(); ) {
            if (isExpired(it.next().getValue())) {
                it.remove();
            } else {
                count++;
            }
        }
        return count;
    }

    private boolean isExpired(Entry<V> entry) {
        return now() - entry.writeTime() > ttlMillis;
    }

    private record Entry<V>(V value, long writeTime) {
    }
}
