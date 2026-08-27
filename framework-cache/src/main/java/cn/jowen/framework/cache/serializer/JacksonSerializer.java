package cn.jowen.framework.cache.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Jackson 序列化器实现，为框架缓存默认序列化方案。
 * 支持泛型类型反序列化，需配合 TypeReference 使用。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class JacksonSerializer implements CacheSerializer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public byte[] serialize(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize object", e);
        }
    }

    @Override
    public @Nullable Object deserialize(byte[] bytes, Class type) {
        try {
            return OBJECT_MAPPER.readValue(bytes, type);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String getType() {
        return "jackson";
    }

    @Override
    public boolean supports(Class type) {
        return true;
    }
}
