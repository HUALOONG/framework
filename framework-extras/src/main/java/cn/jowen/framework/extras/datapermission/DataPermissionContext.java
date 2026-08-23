package cn.jowen.framework.extras.datapermission;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.context.ContextKey;
import cn.jowen.framework.core.context.ContextSnapshot;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * 数据权限上下文：基于 {@code core.context.ContextCarrier} 的当前用户读写与作用域执行。
 *
 * <p>默认 {@code SCOPED_VALUE} 模式下，上下文不跨线程自动继承；异步链路应使用
 * {@link #snapshot()} 捕获并 {@link ContextSnapshot#replay(Runnable)} 回放。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class DataPermissionContext {

    /**
     * 当前用户上下文键。
     */
    public static final ContextKey<UserInfo> CURRENT_USER =
            ContextKey.named("dataPermission.currentUser", UserInfo.class);

    private DataPermissionContext() {
    }

    /**
     * 读取当前用户。
     *
     * @return 当前用户，未设置返回 {@code null}
     */
    public static @Nullable UserInfo getCurrentUser() {
        return ContextCarrier.get(CURRENT_USER);
    }

    /**
     * 写入当前用户（{@code THREAD_LOCAL} 模式下持久生效；{@code SCOPED_VALUE} 模式须处于 {@link #runWith} 作用域内）。
     *
     * @param user 当前用户（可为 null 表示清除）
     */
    public static void setCurrentUser(@Nullable UserInfo user) {
        ContextCarrier.set(CURRENT_USER, user);
    }

    /**
     * 在绑定当前用户的作用域内执行任务。
     *
     * @param user 当前用户（可为 null）
     * @param task 待执行任务
     */
    public static void runWith(@Nullable UserInfo user, Runnable task) {
        Objects.requireNonNull(task, "task must not be null");
        ContextCarrier.runWith(CURRENT_USER, user, task);
    }

    /**
     * 捕获当前上下文快照（可跨线程回放）。
     *
     * @return 快照，不可为 {@code null}
     */
    public static ContextSnapshot snapshot() {
        return ContextCarrier.snapshot();
    }
}
