package cn.jowen.framework.extras.operatelog;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 操作日志上下文：基于 core {@code ContextCarrier} 的当前操作人读写。
 *
 * <p>与 {@link cn.jowen.framework.extras.datapermission.DataPermissionContext}
 * 共享同一套上下文机制，跨虚拟线程迁移时使用 {@code ContextSnapshot}。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class OperateLogContext {

    /**
     * 当前操作人上下文键。
     */
    public static final ContextKey<String> CURRENT_OPERATOR =
            ContextKey.named("operatelog.currentUser", String.class);

    private OperateLogContext() {
    }

    /**
     * 读取当前操作人。
     */
    public static @Nullable String getCurrentOperator() {
        return ContextCarrier.get(CURRENT_OPERATOR);
    }

    /**
     * 设置当前操作人。
     */
    public static void setCurrentOperator(@Nullable String operator) {
        ContextCarrier.set(CURRENT_OPERATOR, operator);
    }
}
