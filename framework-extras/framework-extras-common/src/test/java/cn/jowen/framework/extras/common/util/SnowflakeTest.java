package cn.jowen.framework.extras.common.util;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * {@link Snowflake} 测试。
 *
 * <p>覆盖：nodeId 边界校验、ID 唯一性、单调递增、位段布局、序列溢出跨毫秒、时钟回拨拒绝。
 */
class SnowflakeTest {

    private static final long SEQUENCE_BITS = 12L;
    private static final long NODE_BITS = 10L;
    private static final long MAX_NODE = ~(-1L << NODE_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);

    // ---------- 构造与参数校验 ----------

    @Test
    void constructor_acceptsBoundaryNodeIds() {
        assertThat(new Snowflake(0).nextId()).isPositive();
        assertThat(new Snowflake(MAX_NODE).nextId()).isPositive();
    }

    @Test
    void constructor_rejectsNegativeNodeId() {
        assertThatThrownBy(() -> new Snowflake(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodeId");
    }

    @Test
    void constructor_rejectsNodeIdAboveMax() {
        assertThatThrownBy(() -> new Snowflake(MAX_NODE + 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nodeId");
    }

    // ---------- 唯一性与单调性 ----------

    @Test
    void nextId_isUniqueForManySequentialCalls() {
        Snowflake snowflake = new Snowflake(1);
        int count = 20_000;
        Set<Long> ids = new HashSet<>(count * 2);
        for (int i = 0; i < count; i++) {
            assertThat(ids.add(snowflake.nextId())).as("第 %d 个 ID 重复", i).isTrue();
        }
        assertThat(ids).hasSize(count);
    }

    @Test
    void nextId_isStrictlyIncreasing() {
        Snowflake snowflake = new Snowflake(7);
        long previous = snowflake.nextId();
        for (int i = 0; i < 5_000; i++) {
            long current = snowflake.nextId();
            assertThat(current).isGreaterThan(previous);
            previous = current;
        }
    }

    @Test
    void nextId_isUniqueUnderConcurrency() throws Exception {
        Snowflake snowflake = new Snowflake(512);
        int threads = 8;
        int perThread = 1_000;
        Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int t = 0; t < threads; t++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            ids.add(snowflake.nextId());
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }
        assertThat(ids).hasSize(threads * perThread);
    }

    // ---------- 位段布局 ----------

    @Test
    void nextId_encodesNodeIdInDedicatedBits() {
        long nodeId = 123L;
        Snowflake snowflake = new Snowflake(nodeId);
        for (int i = 0; i < 100; i++) {
            long id = snowflake.nextId();
            long decodedNode = (id >> SEQUENCE_BITS) & MAX_NODE;
            assertThat(decodedNode).isEqualTo(nodeId);
        }
    }

    @Test
    void nextId_differentNodesProduceDifferentIdsInSameMillis() {
        Snowflake a = new Snowflake(1);
        Snowflake b = new Snowflake(2);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            ids.add(a.nextId());
            ids.add(b.nextId());
        }
        assertThat(ids).hasSize(1_000);
    }

    // ---------- 序列溢出 ----------

    @Test
    void nextId_waitsForNextMillisWhenSequenceOverflows() throws Exception {
        Snowflake snowflake = new Snowflake(3);
        long baseline = snowflake.nextId();
        long baselineTimestamp = baseline >>> (NODE_BITS + SEQUENCE_BITS);

        // 人为把序列推到本毫秒最后一个可用值，迫使下一次调用等待进入下一毫秒
        setField(snowflake, "sequence", MAX_SEQUENCE);

        long next = snowflake.nextId();
        long nextTimestamp = next >>> (NODE_BITS + SEQUENCE_BITS);
        long nextSequence = next & MAX_SEQUENCE;

        assertThat(nextTimestamp).isGreaterThan(baselineTimestamp);
        assertThat(nextSequence).isZero();
        assertThat(next).isGreaterThan(baseline);
    }

    // ---------- 时钟回拨 ----------

    @Test
    void nextId_rejectsClockMovedBackwards() throws Exception {
        Snowflake snowflake = new Snowflake(5);
        snowflake.nextId();
        // 把 lastTimestamp 推到未来，等价于系统时钟被回拨
        setField(snowflake, "lastTimestamp", System.currentTimeMillis() + 60_000L);

        assertThatThrownBy(snowflake::nextId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("时钟回拨");
    }

    @Test
    void nextId_recoversAfterClockCatchesUp() throws Exception {
        Snowflake snowflake = new Snowflake(6);
        long before = snowflake.nextId();
        setField(snowflake, "lastTimestamp", System.currentTimeMillis() + 50L);
        assertThatThrownBy(snowflake::nextId).isInstanceOf(IllegalStateException.class);

        // 等待真实时钟追上被篡改的未来时间戳后恢复生成（替代固定 Thread.sleep，容忍抖动）
        long after = await().atMost(Duration.ofSeconds(2)).until(() -> {
            try {
                return snowflake.nextId();
            } catch (IllegalStateException ex) {
                return null;
            }
        }, c -> c != null);
        assertThat(after).isGreaterThan(before);
    }

    private static void setField(Snowflake target, String name, long value) throws Exception {
        Field field = Snowflake.class.getDeclaredField(name);
        field.setAccessible(true);
        field.setLong(target, value);
    }
}
