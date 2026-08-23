package cn.jowen.framework.data.mybatis.repository;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@NullMarked
public final class IdGeneratorAdapter {

    private final Strategy strategy;

    private IdGeneratorAdapter(Strategy strategy) { this.strategy = strategy; }

    public Object generate() { return strategy.nextId(); }

    public static IdGeneratorAdapter autoIncrement() {
        return new IdGeneratorAdapter(SEQUENCE::incrementAndGet);
    }

    public static IdGeneratorAdapter snowflake() {
        return new IdGeneratorAdapter(() -> {
            long timestamp = System.currentTimeMillis() - EPOCH;
            long seq = SEQ.getAndUpdate(s -> (s + 1) & MAX_SEQ);
            return (timestamp << 22L) | seq;
        });
    }

    public static IdGeneratorAdapter uuid() {
        return new IdGeneratorAdapter(() -> UUID.randomUUID().toString().replace("-", ""));
    }

    public static IdGeneratorAdapter custom(Strategy strategy) {
        return new IdGeneratorAdapter(strategy);
    }

    @FunctionalInterface
    public interface Strategy { Object nextId(); }

    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    private static final long EPOCH = 1704067200000L;
    private static final long MAX_SEQ = (1L << 12) - 1;
    private static final AtomicLong SEQ = new AtomicLong(0);
}
