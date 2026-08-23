package cn.jowen.framework.extras.operatelog;

import org.jspecify.annotations.NullMarked;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 操作日志异步分发器：将一条记录并发分发给所有已注册 {@link OperateLogHandler}。
 *
 * <p>零依赖（不引用 {@code core.event}），使用 Java 21 虚拟线程池
 * （{@link Executors#newVirtualThreadPerTaskExecutor()}）执行；提供 {@link #shutdown()} 钩子。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class OperateLogDispatcher {

    private final List<OperateLogHandler> handlers;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public OperateLogDispatcher(Collection<OperateLogHandler> handlers) {
        Objects.requireNonNull(handlers, "handlers must not be null");
        this.handlers = List.copyOf(handlers);
    }

    public OperateLogDispatcher(OperateLogHandler... handlers) {
        this(List.of(handlers));
    }

    /**
     * 异步分发记录给所有处理器。
     *
     * @param record 日志记录（不可为 null）
     */
    public void dispatch(OperateLogRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        for (OperateLogHandler handler : handlers) {
            executor.submit(() -> handler.handle(record));
        }
    }

    /**
     * 同步分发记录给所有处理器。
     *
     * <p>在当前线程顺序调用所有 {@link OperateLogHandler#handle}，适用于 {@code @OperateLog(async=false)} 场景。
     * 与 {@link #dispatch} 的区别是此方法不提交到虚拟线程池，而是同步执行。
     *
     * @param record 日志记录（不可为 null）
     */
    public void dispatchSync(OperateLogRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        for (OperateLogHandler handler : handlers) {
            handler.handle(record);
        }
    }

    /**
     * 关闭分发器：禁止新任务并等待已提交任务完成（最长 30s）。
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
