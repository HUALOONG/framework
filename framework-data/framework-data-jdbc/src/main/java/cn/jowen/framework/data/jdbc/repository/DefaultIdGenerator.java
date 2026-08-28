package cn.jowen.framework.data.jdbc.repository;

import cn.jowen.framework.data.core.mapping.EntityMetadata;
import cn.jowen.framework.core.spi.SPIImplementation;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 默认主键生成器（SPI 实现）。
 *
 * <p>按 {@link Strategy} 生成主键值：
 * <ul>
 *   <li>{@code AUTO}：返回 {@code null}，交由数据库自增（仓库层不写该列）；</li>
 *   <li>{@code UUID}：返回随机 UUID 字符串；</li>
 *   <li>{@code SNOWFLAKE}：返回雪花算法生成的长整型；</li>
 *   <li>{@code SEQUENCE}：以进程内 {@link AtomicLong} 自增模拟（无数据库序列支持时的折中）；</li>
 *   <li>{@code ASSIGNED}：返回 {@code null}，使用实体已赋值的主键。</li>
 * </ul>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@SPIImplementation(name = "default")
@NullMarked
public final class DefaultIdGenerator implements IdGenerator {

    private final Strategy strategy;
    private final Snowflake snowflake = new Snowflake(1L);
    private final AtomicLong sequence = new AtomicLong(0L);

    public DefaultIdGenerator() {
        this(Strategy.AUTO);
    }

    public DefaultIdGenerator(Strategy strategy) {
        this.strategy = strategy;
    }

    @Override
    public @Nullable Object generate(EntityMetadata meta, Object entity) {
        return switch (strategy) {
            case AUTO -> null;
            case UUID -> UUID.randomUUID().toString();
            case SNOWFLAKE -> snowflake.nextId();
            case SEQUENCE -> sequence.incrementAndGet();
            case ASSIGNED -> null;
        };
    }

    /**
     * 雪花算法实现：时间戳(41) + 机器位(10) + 序列位(12)，可排序、趋势递增。
     */
    static final class Snowflake {
        private static final long EPOCH = 1_700_000_000_000L;
        private static final long WORKER_BITS = 10L;
        private static final long SEQUENCE_BITS = 12L;
        private static final long MAX_WORKER = ~(-1L << WORKER_BITS);
        private static final long TIMESTAMP_SHIFT = WORKER_BITS + SEQUENCE_BITS;
        private static final long WORKER_SHIFT = SEQUENCE_BITS;

        private final long workerId;
        private long lastTimestamp = -1L;
        private long sequenceInMillis = 0L;

        Snowflake(long workerId) {
            if (workerId < 0 || workerId > MAX_WORKER) {
                throw new IllegalArgumentException("workerId 越界: " + workerId);
            }
            this.workerId = workerId;
        }

        synchronized long nextId() {
            long timestamp = System.currentTimeMillis();
            if (timestamp < lastTimestamp) {
                throw new IllegalStateException("时钟回拨，拒绝生成 ID");
            }
            if (timestamp == lastTimestamp) {
                sequenceInMillis = (sequenceInMillis + 1) & ~(-1L << SEQUENCE_BITS);
                if (sequenceInMillis == 0) {
                    timestamp = waitNextMillis(lastTimestamp);
                }
            } else {
                sequenceInMillis = 0;
            }
            lastTimestamp = timestamp;
            return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                    | (workerId << WORKER_SHIFT)
                    | sequenceInMillis;
        }

        private long waitNextMillis(long last) {
            long ts = System.currentTimeMillis();
            while (ts <= last) {
                ts = System.currentTimeMillis();
            }
            return ts;
        }
    }
}
