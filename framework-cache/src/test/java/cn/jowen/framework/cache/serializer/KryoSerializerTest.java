package cn.jowen.framework.cache.serializer;

import com.esotericsoftware.kryo.Kryo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link KryoSerializer} 单元测试。
 *
 * <p>覆盖：默认构造与带参构造、序列化/反序列化往返（String / POJO 含集合字段 / 数组）、
 * 反序列化 null 与空字节返回 null 的分支、元数据方法（getType / supports）。
 *
 * <p>为避免 Kryo 默认要求显式注册类导致未注册类型抛异常，测试用 Kryo 显式关闭
 * {@code registrationRequired}；仅用于覆盖序列化逻辑，不影响生产单测目标。
 */
class KryoSerializerTest {

    public static final class SamplePojo {
        private String name;
        private int value;
        private List<String> tags;

        public SamplePojo() {
        }

        public SamplePojo(String name, int value, List<String> tags) {
            this.name = name;
            this.value = value;
            this.tags = tags;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SamplePojo)) {
                return false;
            }
            SamplePojo that = (SamplePojo) o;
            return value == that.value
                    && Objects.equals(name, that.name)
                    && Objects.equals(tags, that.tags);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value, tags);
        }
    }

    private KryoSerializer serializerWithRegistrationOff() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
        return new KryoSerializer(kryo);
    }

    @Test
    void defaultConstructor_instantiableAndExposesType() {
        KryoSerializer serializer = new KryoSerializer();
        assertThat(serializer).isNotNull();
        assertThat(serializer.getType()).isEqualTo("kryo");
    }

    @Test
    void serialize_string_roundTrip() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        byte[] bytes = serializer.serialize("hello");
        assertThat(bytes).isNotEmpty();
        Object result = serializer.deserialize(bytes, String.class);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void serialize_pojo_roundTrip() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        SamplePojo pojo = new SamplePojo("jowen", 42, Arrays.asList("a", "b"));
        byte[] bytes = serializer.serialize(pojo);
        SamplePojo result = (SamplePojo) serializer.deserialize(bytes, SamplePojo.class);
        assertThat(result.getName()).isEqualTo("jowen");
        assertThat(result.getValue()).isEqualTo(42);
        assertThat(result.getTags()).containsExactly("a", "b");
    }

    @Test
    void serialize_integerArray_roundTrip() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        Integer[] data = {1, 2, 3};
        byte[] bytes = serializer.serialize(data);
        Object result = serializer.deserialize(bytes, Integer[].class);
        assertThat(result).isEqualTo(data);
    }

    @Test
    void deserialize_nullBytes_returnsNull() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        Object result = serializer.deserialize(null, String.class);
        assertThat(result).isNull();
    }

    @Test
    void deserialize_emptyBytes_returnsNull() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        Object result = serializer.deserialize(new byte[0], String.class);
        assertThat(result).isNull();
    }

    @Test
    void getType_returnsKryo() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        assertThat(serializer.getType()).isEqualTo("kryo");
    }

    @Test
    void supports_anyType_returnsTrue() {
        KryoSerializer serializer = serializerWithRegistrationOff();
        assertThat(serializer.supports(String.class)).isTrue();
        assertThat(serializer.supports(SamplePojo.class)).isTrue();
    }
}
