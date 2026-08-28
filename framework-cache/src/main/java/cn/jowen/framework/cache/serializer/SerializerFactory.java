package cn.jowen.framework.cache.serializer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化器工厂。按类型名称创建或获取 {@link CacheSerializer} 实例，支持 SPI 注册。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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

    /**
     * 按类型名称获取已注册的序列化器。
     *
     * @param type 序列化器类型名（如 {@code jackson}、{@code kryo}）
     * @return 对应的 {@link CacheSerializer}；若类型未注册则返回 {@code null}，
     *         调用方必须判空或改用 {@link #getDefault()} 获取兜底实现
     */
    public static @Nullable CacheSerializer create(String type) {
        return REGISTERED.get(type);
    }

    public static CacheSerializer getDefault() {
        return REGISTERED.get("jackson");
    }
}
