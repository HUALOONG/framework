package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * {@link DefaultIdGenerator} 测试。
 */
class DefaultIdGeneratorTest {

    private final EntityMetadata meta = mock(EntityMetadata.class);

    @Test
    void auto_returnsNull() {
        DefaultIdGenerator generator = new DefaultIdGenerator(DefaultIdGenerator.Strategy.AUTO);
        assertThat(generator.generate(meta, new Object())).isNull();
    }

    @Test
    void assigned_returnsNull() {
        DefaultIdGenerator generator = new DefaultIdGenerator(DefaultIdGenerator.Strategy.ASSIGNED);
        assertThat(generator.generate(meta, new Object())).isNull();
    }

    @Test
    void uuid_returnsUuidString() {
        DefaultIdGenerator generator = new DefaultIdGenerator(DefaultIdGenerator.Strategy.UUID);
        Object id = generator.generate(meta, new Object());
        assertThat(id).isInstanceOf(String.class);
        assertThat(UUID.fromString((String) id)).isNotNull();
    }

    @Test
    void sequence_increments() {
        DefaultIdGenerator generator = new DefaultIdGenerator(DefaultIdGenerator.Strategy.SEQUENCE);
        assertThat(generator.generate(meta, new Object())).isEqualTo(1L);
        assertThat(generator.generate(meta, new Object())).isEqualTo(2L);
    }

    @Test
    void snowflake_returnsIncreasingIds() {
        DefaultIdGenerator generator = new DefaultIdGenerator(DefaultIdGenerator.Strategy.SNOWFLAKE);
        long first = (Long) generator.generate(meta, new Object());
        long second = (Long) generator.generate(meta, new Object());
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void snowflake_defaultConstructor_auto() {
        DefaultIdGenerator generator = new DefaultIdGenerator();
        assertThat(generator.generate(meta, new Object())).isNull();
    }

    @Test
    void snowflake_internal_rejectsInvalidWorkerId() {
        assertThatThrownBy(() -> new DefaultIdGenerator.Snowflake(-1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultIdGenerator.Snowflake(1024))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snowflake_internal_validWorkerId() {
        DefaultIdGenerator.Snowflake snowflake = new DefaultIdGenerator.Snowflake(0L);
        assertThat(snowflake.nextId()).isPositive();
        assertThat(snowflake.nextId()).isPositive();
    }
}
