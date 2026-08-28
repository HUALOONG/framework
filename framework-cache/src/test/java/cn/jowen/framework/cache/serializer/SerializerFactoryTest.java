package cn.jowen.framework.cache.serializer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SerializerFactory} 单元测试。
 *
 * <p>覆盖：静态注册块（jackson / kryo）、按类型名获取（命中 / 未注册返回 null）、
 * 默认实现获取，以及两种 {@code register} 重载（按实现自带类型名、显式指定类型名）。
 */
class SerializerFactoryTest {

    /** 简单的测试替身：手写实现 {@link CacheSerializer}，无需 Mockito。 */
    static final class DummySerializer implements CacheSerializer {
        @Override
        public byte[] serialize(Object object) {
            return new byte[0];
        }

        @Override
        public Object deserialize(byte[] bytes, Class type) {
            return null;
        }

        @Override
        public String getType() {
            return "dummy";
        }

        @Override
        public boolean supports(Class type) {
            return true;
        }
    }

    @Test
    void create_jackson_returnsJacksonSerializer() {
        CacheSerializer serializer = SerializerFactory.create("jackson");
        assertThat(serializer).isInstanceOf(JacksonSerializer.class);
        assertThat(serializer.getType()).isEqualTo("jackson");
    }

    @Test
    void create_kryo_returnsKryoSerializer() {
        CacheSerializer serializer = SerializerFactory.create("kryo");
        assertThat(serializer).isInstanceOf(KryoSerializer.class);
        assertThat(serializer.getType()).isEqualTo("kryo");
    }

    @Test
    void create_unknown_returnsNull() {
        assertThat(SerializerFactory.create("nonexistent")).isNull();
    }

    @Test
    void getDefault_returnsJackson() {
        CacheSerializer serializer = SerializerFactory.getDefault();
        assertThat(serializer).isInstanceOf(JacksonSerializer.class);
    }

    @Test
    void register_withSerializer_registersByOwnType() {
        CacheSerializer dummy = new DummySerializer();
        SerializerFactory.register(dummy);
        assertThat(SerializerFactory.create("dummy")).isSameAs(dummy);
    }

    @Test
    void register_withTypeAndSerializer_registersByName() {
        CacheSerializer dummy = new DummySerializer();
        SerializerFactory.register("custom", dummy);
        assertThat(SerializerFactory.create("custom")).isSameAs(dummy);
    }
}
