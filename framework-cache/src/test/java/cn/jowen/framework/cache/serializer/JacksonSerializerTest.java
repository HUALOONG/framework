package cn.jowen.framework.cache.serializer;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link JacksonSerializer} 单元测试。
 *
 * <p>覆盖：序列化/反序列化往返、null 值、不同类型（String / Integer / POJO 含集合字段）、
 * 反序列化传入 null 或非法字节时返回 null 的异常分支，以及元数据方法（getType / supports）。
 *
 * <p>纯对象↔字节，无外部依赖。Jackson 3（tools.jackson）作为依赖以 {@code optional} 形式提供，
 * 在本模块测试 classpath 上可用。
 */
class JacksonSerializerTest {

    private final JacksonSerializer serializer = new JacksonSerializer();

    /**
     * 用于序列化往返测试的 POJO。提供无参构造、字段访问器与 equals/hashCode，
     * 以便 Jackson 与 Kryo 均能正常实例化并做字段级断言。
     */
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

    @Test
    void serialize_string_roundTrip() {
        byte[] bytes = serializer.serialize("hello");
        assertThat(bytes).isNotEmpty();
        Object result = serializer.deserialize(bytes, String.class);
        assertThat(result).isEqualTo("hello");
    }

    @Test
    void serialize_integer_roundTrip() {
        byte[] bytes = serializer.serialize(123);
        Object result = serializer.deserialize(bytes, Integer.class);
        assertThat(result).isEqualTo(123);
    }

    @Test
    void serialize_pojo_roundTrip() {
        SamplePojo pojo = new SamplePojo("jowen", 42, Arrays.asList("a", "b"));
        byte[] bytes = serializer.serialize(pojo);
        SamplePojo result = (SamplePojo) serializer.deserialize(bytes, SamplePojo.class);
        assertThat(result).isEqualTo(pojo);
        assertThat(result.getName()).isEqualTo("jowen");
        assertThat(result.getValue()).isEqualTo(42);
        assertThat(result.getTags()).containsExactly("a", "b");
    }

    @Test
    void serialize_nullValue_producesNullOnDeserialize() {
        byte[] bytes = serializer.serialize(null);
        assertThat(bytes).isNotNull();
        Object result = serializer.deserialize(bytes, Object.class);
        assertThat(result).isNull();
    }

    @Test
    void deserialize_nullBytes_returnsNull() {
        Object result = serializer.deserialize(null, String.class);
        assertThat(result).isNull();
    }

    @Test
    void deserialize_invalidBytes_returnsNull() {
        Object result = serializer.deserialize(new byte[] {0, 1, 2}, String.class);
        assertThat(result).isNull();
    }

    @Test
    void getType_returnsJackson() {
        assertThat(serializer.getType()).isEqualTo("jackson");
    }

    @Test
    void supports_anyType_returnsTrue() {
        assertThat(serializer.supports(String.class)).isTrue();
        assertThat(serializer.supports(SamplePojo.class)).isTrue();
    }
}
