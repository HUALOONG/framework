package cn.jowen.framework.cache.serializer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂。按类型名称创建或获取 {@link CacheSerializer} 实例，支持 SPI 注册。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class SerializerFactory {

    private static final Map<String, CacheSerializer<?>> REGISTERED = new ConcurrentHashMap<>();

    private SerializerFactory() {
    }

    /**
     * 注册序列化器。
     *
     * @param type       类型名称，不可为 {@code null}
     * @param serializer 序列化器，不可为 {@code null}
     */
    public static void register(String type, CacheSerializer<?> serializer) {
        REGISTERED.put(type, serializer);
    }

    /**
     * 按类型名称创建序列化器；未注册时返回 {@code null}。
     *
     * @param type 类型名称，不可为 {@code null}
     * @return 序列化器或 {@code null}
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> CacheSerializer<T> create(String type) {
        return (CacheSerializer<T>) REGISTERED.get(type);
    }

    /**
     * 获取默认序列化器。当前默认返回 {@code null}，由实现方按需注册。
     *
     * @return 默认序列化器或 {@code null}
     */
    @Nullable
    public static CacheSerializer<?> getDefault() {
        return REGISTERED.get("jackson");
    }
}
