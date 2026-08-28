package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.data.core.meta.GeneratedValue;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * {@link IdGenerators} 单元测试。
 */
class IdGeneratorsTest {

    private final EntityMetadata meta = mock(EntityMetadata.class);

    @Test
    void get_auto_returnsNull() {
        IdGenerator generator = IdGenerators.get(GeneratedValue.Strategy.AUTO);
        assertThat(generator.generate(meta, new Object())).isNull();
    }

    @Test
    void get_uuid_generatesUuid() {
        IdGenerator generator = IdGenerators.get(GeneratedValue.Strategy.UUID);
        Object id = generator.generate(meta, new Object());
        assertThat(id).isInstanceOf(String.class);
        assertThat(UUID.fromString((String) id)).isNotNull();
    }

    @Test
    void get_snowflake_generatesLong() {
        IdGenerator generator = IdGenerators.get(GeneratedValue.Strategy.SNOWFLAKE);
        Object id1 = generator.generate(meta, new Object());
        Object id2 = generator.generate(meta, new Object());
        assertThat(id1).isInstanceOf(Long.class);
        assertThat(id2).isInstanceOf(Long.class);
    }

    @Test
    void get_assigned_returnsNull() {
        IdGenerator generator = IdGenerators.get(GeneratedValue.Strategy.ASSIGNED);
        assertThat(generator.generate(meta, new Object())).isNull();
    }

    @Test
    void snowflake_idsAreIncreasing() throws InterruptedException {
        DefaultIdGenerator generator = new DefaultIdGenerator(IdGenerator.Strategy.SNOWFLAKE);
        long first = (Long) generator.generate(meta, new Object());
        Thread.sleep(2);
        long second = (Long) generator.generate(meta, new Object());
        assertThat(second).isGreaterThan(first);
    }

    @Test
    void snowflake_sequence_generatesDistinctIdsInSameMillis() {
        DefaultIdGenerator generator = new DefaultIdGenerator(IdGenerator.Strategy.SNOWFLAKE);
        long[] ids = new long[10];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = (Long) generator.generate(meta, new Object());
        }
        assertThat(ids).doesNotHaveDuplicates();
        // 同毫秒内序列递增
        for (int i = 1; i < ids.length; i++) {
            assertThat(ids[i]).isGreaterThan(ids[i - 1]);
        }
    }

    @Test
    void snowflake_workerIdOutOfRange_throws() {
        assertThatThrownBy(() -> new DefaultIdGenerator.Snowflake(-1L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DefaultIdGenerator.Snowflake(1L << 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void snowflake_clockRollback_throws() {
        // 构造后将内部 lastTimestamp 置为未来时间，模拟时钟回拨
        DefaultIdGenerator.Snowflake snowflake = new DefaultIdGenerator.Snowflake(1L);
        snowflake.nextId();
        try {
            Field last = DefaultIdGenerator.Snowflake.class.getDeclaredField("lastTimestamp");
            last.setAccessible(true);
            last.setLong(snowflake, System.currentTimeMillis() + 60_000L);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        assertThatThrownBy(snowflake::nextId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("时钟回拨");
    }

    @Test
    void snowflake_sequenceOverflow_waitNextMillis() throws Exception {
        DefaultIdGenerator.Snowflake snowflake = new DefaultIdGenerator.Snowflake(1L);
        Field seq = DefaultIdGenerator.Snowflake.class.getDeclaredField("sequenceInMillis");
        Field last = DefaultIdGenerator.Snowflake.class.getDeclaredField("lastTimestamp");
        seq.setAccessible(true);
        last.setAccessible(true);
        // 将「当前毫秒 + 序列上限」同时置位，确保下一次调用命中同毫秒溢出分支；
        // 若调用时恰好跨毫秒则重试，消除时序竞态
        for (int attempt = 0; attempt < 100; attempt++) {
            last.setLong(snowflake, System.currentTimeMillis());
            seq.setLong(snowflake, 4095L);
            long id = snowflake.nextId();
            if ((id & 4095L) == 0L) {
                return;
            }
        }
        throw new AssertionError("未能在同毫秒内触发序列溢出");
    }

    @Test
    void snowflake_waitNextMillis_spinsUntilNextMillisecond() throws Exception {
        DefaultIdGenerator.Snowflake snowflake = new DefaultIdGenerator.Snowflake(1L);
        java.lang.reflect.Method waitNext = DefaultIdGenerator.Snowflake.class
                .getDeclaredMethod("waitNextMillis", long.class);
        waitNext.setAccessible(true);
        // 传入未来时间戳，确保 while 自旋体至少执行一次
        long last = System.currentTimeMillis() + 5L;
        long next = (long) waitNext.invoke(snowflake, last);
        assertThat(next).isGreaterThan(last);
    }

    @Test
    void get_assigned_generatesId() {
        IdGenerator generator = IdGenerators.get(GeneratedValue.Strategy.ASSIGNED);
        Object id = generator.generate(meta, new Object());
        assertThat(id).isNull();
    }
}
