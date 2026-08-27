package cn.jowen.framework.cache.serializer;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 缓存序列化器接口，支持可插拔的序列化方案（Jackson/Kryo 等）。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public interface CacheSerializer {

    <T> byte[] serialize(T object);

    <T> @Nullable T deserialize(byte[] bytes, Class<T> type);

    String getType();

    boolean supports(Class<?> type);
}
