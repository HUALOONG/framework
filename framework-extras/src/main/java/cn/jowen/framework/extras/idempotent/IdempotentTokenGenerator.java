package cn.jowen.framework.extras.idempotent;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

/**
 * 幂等令牌生成器。
 *
 * <p>支持 UUID 和 Snowflake 两种生成策略。Snowflake 为纯 Java 实现
 * （41 位时间戳 + 10 位 workId + 12 位序列，毫秒内自增序列保证单调且不重复），
 * 不引入任何依赖。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class IdempotentTokenGenerator {

    /** 起始纪元（Twitter 雪花算法惯用，2026-08-27 远大于此）。 */
    private static final long EPOCH = 1288834974657L;

    private static final long WORKER_ID_BITS = 10L;
    private static final long SEQUENCE_BITS = 12L;
    private static final long MAX_WORKER_ID = (1L << WORKER_ID_BITS) - 1L;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1L;
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 单 JVM 默认实例（workId=0），供静态便捷方法复用，保证同 JVM 内单调。 */
    private static final IdempotentTokenGenerator DEFAULT = new IdempotentTokenGenerator(0L);

    private final long workId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    /**
     * 构造雪花生成器。
     *
     * @param workId 机器标识（0~1023）
     */
    public IdempotentTokenGenerator(long workId) {
        if (workId < 0 || workId > MAX_WORKER_ID) {
            throw new IllegalArgumentException("workId 必须位于 [0, 1023]，实际: " + workId);
        }
        this.workId = workId;
    }

    /**
     * 生成 UUID 风格的令牌。
     *
     * @return 32位小写十六进制字符串
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成雪花令牌（使用默认 workId=0）。
     *
     * @return 无符号十进制雪花 ID
     */
    public static String generateSnowflake() {
        return Long.toUnsignedString(DEFAULT.nextId());
    }

    /**
     * 生成雪花令牌（指定 workId，供多实例区分）。
     *
     * @param workId 机器标识（0~1023）
     * @return 无符号十进制雪花 ID
     */
    public static String generateSnowflake(long workId) {
        return Long.toUnsignedString(new IdempotentTokenGenerator(workId).nextId());
    }

    /**
     * 生成下一个雪花 ID。
     *
     * @return 64 位雪花 ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        if (timestamp < lastTimestamp) {
            // 时钟回拨会导致 ID 重复，直接拒绝并抛出
            throw new IllegalStateException("时钟回拨，拒绝生成雪花 ID");
        }
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 同毫秒序列用尽，等待下一毫秒
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (workId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long waitNextMillis(long last) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= last) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
