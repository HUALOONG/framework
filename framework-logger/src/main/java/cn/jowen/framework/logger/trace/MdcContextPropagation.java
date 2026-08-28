package cn.jowen.framework.logger.trace;

import cn.jowen.framework.core.context.ContextPropagator;
import cn.jowen.framework.core.context.ContextSnapshot;
import org.jspecify.annotations.NullMarked;

/**
 * 基于 {@link ContextSnapshot} 的上下文传播器实现。
 *
 * <p>将 {@link TraceContext} 中的 traceId/spanId 快照带入异步任务（线程池/虚拟线程调度器），
 * 保证跨线程迁移时链路不丢。业务方通过 {@link cn.jowen.framework.core.spi.ExtensionLoader}
 * 自动发现，或手动包装异步任务使用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class MdcContextPropagation implements ContextPropagator {
    /**
     * 包装异步任务。
     *
     * @param task 异步任务，不可为 {@code null}
     * @return 包装后的异步任务，不可为 {@code null}
     */
    @Override
    public Runnable wrap(Runnable task) {
        ContextSnapshot snapshot = ContextSnapshot.capture();
        return () -> snapshot.replay(task);
    }
}
