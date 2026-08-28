package cn.jowen.framework.cache.serializer;

import tools.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Jackson 3 序列化器实现，为框架缓存默认序列化方案。
 * 支持泛型类型反序列化，需配合 TypeReference 使用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class JacksonSerializer implements CacheSerializer {

    private static final JsonMapper OBJECT_MAPPER = JsonMapper.builder().build();

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
