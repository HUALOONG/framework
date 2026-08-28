package cn.jowen.framework.cache.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * 基于 Kryo 的高性能序列化实现，适用于内存缓存（如 Caffeine）等可接受反序列化安全的场景。
 *
 * <p>Kryo 序列化性能优于 Jackson，但不支持跨版本兼容，且需提前注册类。本实现使用 {@link Kryo#getDefault()} 简化注册，
 * 适合纯内存缓存场景。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class KryoSerializer implements CacheSerializer {

    private final Kryo kryo;

    public KryoSerializer() {
        this.kryo = new Kryo();
    }

    public KryoSerializer(Kryo kryo) {
        this.kryo = kryo;
    }

    @Override
    public byte[] serialize(Object object) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (Output output = new Output(baos)) {
            kryo.writeClassAndObject(output, object);
            output.flush();
        }
        return baos.toByteArray();
    }

    @Override
    public @Nullable Object deserialize(byte[] bytes, Class type) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try (Input input = new Input(new ByteArrayInputStream(bytes))) {
            return kryo.readClassAndObject(input);
        }
    }

    @Override
    public String getType() {
        return "kryo";
    }

    @Override
    public boolean supports(Class type) {
        return true;
    }
}
