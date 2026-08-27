package cn.jowen.framework.cache.serializer;

import org.jspecify.annotations.NullMarked;

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

    private static final Map<String, CacheSerializer> REGISTERED = new ConcurrentHashMap<>();

    static {
        register(new JacksonSerializer());
        register(new KryoSerializer());
    }

    private SerializerFactory() {
    }

    public static void register(CacheSerializer serializer) {
        REGISTERED.put(serializer.getType(), serializer);
    }

    public static void register(String type, CacheSerializer serializer) {
        REGISTERED.put(type, serializer);
    }

    public static CacheSerializer create(String type) {
        return REGISTERED.get(type);
    }

    public static CacheSerializer getDefault() {
        return REGISTERED.get("jackson");
    }
}
