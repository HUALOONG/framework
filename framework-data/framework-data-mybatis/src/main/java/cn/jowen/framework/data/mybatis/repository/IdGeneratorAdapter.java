package cn.jowen.framework.data.mybatis.repository;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@NullMarked
/**
 * 「IdGeneratorAdapter」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public final class IdGeneratorAdapter {

    /** strategy 不可变字段。 */
    private final Strategy strategy;

    /** private 字段。 */
    private IdGeneratorAdapter(Strategy strategy) { this.strategy = strategy; }

    /** return 字段。 */
    public Object generate() { return strategy.nextId(); }

    /**
     * 执行auto increment操作。
     * @return 结果
     */
    public static IdGeneratorAdapter autoIncrement() {
        return new IdGeneratorAdapter(SEQUENCE::incrementAndGet);
    }

    /**
     * 执行snowflake操作。
     * @return 结果
     */
    public static IdGeneratorAdapter snowflake() {
        return new IdGeneratorAdapter(() -> {
            long timestamp = System.currentTimeMillis() - EPOCH;
            long seq = SEQ.getAndUpdate(s -> (s + 1) & MAX_SEQ);
            return (timestamp << 22L) | seq;
        });
    }

    /**
     * 执行uuid操作。
     * @return 结果
     */
    public static IdGeneratorAdapter uuid() {
        return new IdGeneratorAdapter(() -> UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 执行custom操作。
     * @param strategy 参数 strategy
     * @return 结果
     */
    public static IdGeneratorAdapter custom(Strategy strategy) {
        return new IdGeneratorAdapter(strategy);
    }

    /** Object 字段。 */
    @FunctionalInterface
    /**
     * 「Strategy」接口定义。
     *
     * @author Jowen
     * @since 0.0.1
 * @version 0.0.1
     */
    public interface Strategy { Object nextId(); }

    /** SEQUENCE 常量。 */
    /** SEQUENCE 常量。 */
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    /** EPOCH 常量。 */
    /** EPOCH 常量。 */
    private static final long EPOCH = 1704067200000L;
    /** MAX_SEQ 常量。 */
    /** MAX_SEQ 常量。 */
    private static final long MAX_SEQ = (1L << 12) - 1;
    /** SEQ 常量。 */
    /** SEQ 常量。 */
    private static final AtomicLong SEQ = new AtomicLong(0);
}
