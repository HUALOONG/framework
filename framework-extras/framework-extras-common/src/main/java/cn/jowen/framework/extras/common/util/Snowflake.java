package cn.jowen.framework.extras.common.util;

import org.jspecify.annotations.NullMarked;

/**
 * Snowflake 分布式 ID 生成器。
 *
 * <p>结构：{@code 1 位符号位 | 41 位时间戳 | 10 位节点位 | 12 位序列位}。
 * 默认每节点每毫秒可生成 4096 个 ID，理论可用 69 年。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Snowflake {

    private static final long EPOCH = 1_700_000_000_000L; // 2023-11-14  baseline
    private static final long NODE_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_NODE = ~(-1L << NODE_BITS);
    private static final long MAX_SEQUENCE = ~(-1L << SEQUENCE_BITS);
    private static final long NODE_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = NODE_BITS + SEQUENCE_BITS;

    private final long nodeId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public Snowflake(long nodeId) {
        if (nodeId < 0 || nodeId > MAX_NODE) {
            throw new IllegalArgumentException("nodeId 必须在 [0, " + MAX_NODE + "] 范围内");
        }
        this.nodeId = nodeId;
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            throw new IllegalStateException("时钟回拨，拒绝生成 ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (nodeId << NODE_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
