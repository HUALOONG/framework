package cn.jowen.framework.cache;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 组合缓存（本地 + 后备）。本地层未命中时查后备层，命中则回填本地，兼顾性能与一致性。
 *
 * <p>防护能力：
 * <ul>
 *   <li><b>缓存穿透</b>：对后备层也不存在的值记录到 {@code knownNulls}（可配置占位 TTL），避免重复击穿到数据源。</li>
 *   <li><b>缓存击穿</b>：同一 key 的加载过程加单飞锁，仅一个线程回源，其余等待复用结果。</li>
 * </ul>
 *
 * @param <K> 键类型
 * @param <V> 值类型
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class LayeredCache<K, V> extends AbstractCache<K, V> {

    private final Cache<K, V> local;
    private final Cache<K, V> backup;
    /** 单飞锁：按 key 串行化回源。 */
    private final ConcurrentMap<K, Object> loadLocks = new ConcurrentHashMap<>();
    /** 已知空 key 及其写入时间（毫秒），用于穿透防护。 */
    private final ConcurrentMap<K, Long> knownNulls = new ConcurrentHashMap<>();
    /** 空值占位存活毫秒；{@code <=0} 表示不缓存空值。 */
    private final long nullTtlMillis;

    public LayeredCache(String name, Cache<K, V> local, Cache<K, V> backup, long nullTtlMillis) {
        super(name);
        this.local = local;
        this.backup = backup;
        this.nullTtlMillis = nullTtlMillis;
    }

    @Override
    protected @Nullable V doGet(K key) {
        V localVal = local.get(key);
        if (localVal != null) {
            return localVal;
        }
        if (nullTtlMillis > 0 && isKnownNull(key)) {
            return null; // 穿透防护命中
        }
        // 单飞回源
        Object lock = loadLocks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            try {
                V fresh = local.get(key);
                if (fresh != null) {
                    return fresh;
                }
                if (nullTtlMillis > 0 && isKnownNull(key)) {
                    return null;
                }
                V backupVal = backup.get(key);
                if (backupVal != null) {
                    local.put(key, backupVal);
                    return backupVal;
                }
                // 后备层也无：记录空占位
                if (nullTtlMillis > 0) {
                    knownNulls.put(key, System.currentTimeMillis());
                }
                return null;
            } finally {
                loadLocks.remove(key, lock);
            }
        }
    }

    @Override
    protected void doPut(K key, V value) {
        knownNulls.remove(key);
        local.put(key, value);
        backup.put(key, value);
    }

    @Override
    protected void doEvict(K key) {
        knownNulls.remove(key);
        local.evict(key);
        backup.evict(key);
    }

    @Override
    protected void doClear() {
        knownNulls.clear();
        local.clear();
        backup.clear();
    }

    @Override
    protected long doSize() {
        return local.size();
    }

    private boolean isKnownNull(K key) {
        Long ts = knownNulls.get(key);
        if (ts == null) {
            return false;
        }
        if (System.currentTimeMillis() - ts > nullTtlMillis) {
            knownNulls.remove(key, ts);
            return false;
        }
        return true;
    }

    /**
     * 便捷方法：未命中时回源并写入。等价于 {@code get(key) ?? loader.apply(key)}（回填两层）。
     *
     * @param key    键，不可为 {@code null}
     * @param loader 回源函数，不可为 {@code null}
     * @return 值，不可为 {@code null}
     */
    public V computeIfAbsent(K key, Function<? super K, ? extends @Nullable V> loader) {
        V existing = get(key);
        if (existing != null) {
            return existing;
        }
        // 穿透防护：已知空 key 直接返回，不再回源
        if (nullTtlMillis > 0 && isKnownNull(key)) {
            return null;
        }
        V loaded = loader.apply(key);
        if (loaded != null) {
            put(key, loaded);
        }
        return loaded;
    }
}
