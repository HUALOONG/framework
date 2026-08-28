package cn.jowen.framework.cache.eviction;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 淘汰策略工厂，按类型名注册与获取 {@link EvictionPolicy} 实例，默认提供 LRU。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class EvictionPolicyFactory {

    private static final Map<String, EvictionPolicy> REGISTERED = new ConcurrentHashMap<>();

    static {
        register("lru", new LruEvictionPolicy());
    }

    private EvictionPolicyFactory() {
    }

    public static void register(String type, EvictionPolicy policy) {
        REGISTERED.put(type, policy);
    }

    @Nullable
    public static EvictionPolicy create(String type) {
        return REGISTERED.get(type);
    }

    public static EvictionPolicy getDefault() {
        return REGISTERED.getOrDefault("lru", EvictionPolicy.LRU);
    }
}
