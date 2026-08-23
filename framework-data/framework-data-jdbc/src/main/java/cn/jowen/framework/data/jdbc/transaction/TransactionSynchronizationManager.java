package cn.jowen.framework.data.jdbc.transaction;

import cn.jowen.framework.data.jdbc.connection.ConnectionHolder;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 事务同步管理器：基于 {@link ThreadLocal} 在事务线程内绑定连接、记录同步回调并驱动其生命周期。
 *
 * <p>本类不依赖任何外部容器，配合 {@link JdbcTransactionManager} 实现本地事务的资源绑定与传播。
 * 同步回调（{@link TransactionSynchronization}）在提交/完成后被触发，用于资源清理或后置通知。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class TransactionSynchronizationManager {

    private static final ThreadLocal<@Nullable ConnectionHolder> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<@Nullable List<TransactionSynchronization>> SYNCHRONIZATIONS = new ThreadLocal<>();

    private TransactionSynchronizationManager() {
    }

    /**
     * 取当前线程绑定的连接持有者。
     *
     * @return 连接持有者，未绑定返回 {@code null}
     */
    public static @Nullable ConnectionHolder getConnectionHolder() {
        return HOLDER.get();
    }

    /**
     * 绑定连接持有者到当前线程。
     *
     * @param holder 连接持有者，不可为 {@code null}
     */
    public static void bindResource(ConnectionHolder holder) {
        HOLDER.set(holder);
        ACTIVE.set(Boolean.TRUE);
    }

    /**
     * 解绑当前线程的连接持有者并清理同步回调。
     */
    public static void unbindResource() {
        HOLDER.remove();
        ACTIVE.remove();
        List<TransactionSynchronization> syncs = SYNCHRONIZATIONS.get();
        if (syncs != null) {
            syncs.clear();
        }
        SYNCHRONIZATIONS.remove();
    }

    /**
     * 当前线程是否处于激活的事务同步状态。
     *
     * @return 激活返回 {@code true}
     */
    public static boolean isSynchronizationActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    /**
     * 注册事务同步回调。
     *
     * @param synchronization 回调，不可为 {@code null}
     */
    public static void registerSynchronization(TransactionSynchronization synchronization) {
        List<TransactionSynchronization> syncs = SYNCHRONIZATIONS.get();
        if (syncs == null) {
            syncs = new ArrayList<>();
            SYNCHRONIZATIONS.set(syncs);
        }
        syncs.add(synchronization);
    }

    /**
     * 取当前注册的同步回调（不可变快照）。
     *
     * @return 回调列表
     */
    public static List<TransactionSynchronization> getSynchronizations() {
        List<TransactionSynchronization> syncs = SYNCHRONIZATIONS.get();
        return syncs == null ? List.of() : List.copyOf(syncs);
    }

    /**
     * 触发所有同步回调的「提交前」钩子。
     */
    public static void triggerBeforeCommit() {
        for (TransactionSynchronization sync : getSynchronizations()) {
            sync.beforeCommit();
        }
    }

    /**
     * 触发所有同步回调的「提交后」钩子。
     */
    public static void triggerAfterCommit() {
        for (TransactionSynchronization sync : getSynchronizations()) {
            sync.afterCommit();
        }
    }

    /**
     * 触发所有同步回调的「完成后」钩子。
     */
    public static void triggerAfterCompletion() {
        for (TransactionSynchronization sync : getSynchronizations()) {
            sync.afterCompletion();
        }
    }
}
