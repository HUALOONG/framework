package cn.jowen.framework.cache.serializer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 缓存序列化器接口。支持可插拔的序列化方案（Jackson/Kryo/Hessian 等）。
 *
 * @param <T> 序列化目标类型
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public interface CacheSerializer<T> {

    /**
     * 将对象序列化为字节数组。
     *
     * @param object 待序列化对象，不可为 {@code null}
     * @return 字节数组，不可为 {@code null}
     */
    byte[] serialize(T object);

    /**
     * 将字节数组反序列化为对象。
     *
     * @param bytes 字节数组，不可为 {@code null}
     * @param type  目标类型，不可为 {@code null}
     * @return 反序列化结果，可能为 {@code null}
     */
    @Nullable T deserialize(byte[] bytes, Class<T> type);

    /**
     * @return 序列化类型名称
     */
    String getType();

    /**
     * 判断是否支持指定类型。
     *
     * @param type 待检查类型，不可为 {@code null}
     * @return {@code true} 表示支持
     */
    boolean supports(Class<?> type);
}
