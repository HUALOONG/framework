package cn.jowen.framework.data.core.transaction;

import org.jspecify.annotations.NullMarked;

/**
 * 事务定义，含传播行为、隔离级别、超时与只读标记。
 *
 * @author 王飞
 * @since 2026-08-21
 */
@NullMarked
public final class TransactionDefinition {

    private final Propagation propagation;
    private final Isolation isolation;
    private final int timeout; // 秒，<0 表示不限
    private final boolean readOnly;

    public TransactionDefinition(Propagation propagation, Isolation isolation, int timeout, boolean readOnly) {
        this.propagation = propagation;
        this.isolation = isolation;
        this.timeout = timeout;
        this.readOnly = readOnly;
    }

    /**
     * 默认定义：REQUIRED 传播、数据库默认隔离、无超时、非只读。
     *
     * @return 默认定义
     */
    public static TransactionDefinition defaults() {
        return new TransactionDefinition(Propagation.REQUIRED, Isolation.DEFAULT, -1, false);
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public Isolation getIsolation() {
        return isolation;
    }

    public int getTimeout() {
        return timeout;
    }

    public boolean isReadOnly() {
        return readOnly;
    }
}
