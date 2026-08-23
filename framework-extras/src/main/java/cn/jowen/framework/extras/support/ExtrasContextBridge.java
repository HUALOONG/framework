package cn.jowen.framework.extras.support;

import cn.jowen.framework.core.context.ContextCarrier;
import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Extras 上下文桥接器（非 Web 版本）。
 *
 * <p>提供与 core {@link ContextCarrier} 的简单桥接，支持操作日志和数据权限模块获取上下文。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public final class ExtrasContextBridge {

    private static final ContextKey<String> OPERATOR_KEY =
            ContextKey.named("extras:operator", String.class);

    private ExtrasContextBridge() {
    }

    /**
     * 设置当前登录用户。
     *
     * @param operator 用户名，可为 {@code null}（表示清除）
     */
    public static void setOperator(@Nullable String operator) {
        ContextCarrier.set(OPERATOR_KEY, operator);
    }

    /**
     * 获取当前登录用户。
     *
     * @return 用户名，未设置返回 {@code null}
     */
    @Nullable
    public static String getOperator() {
        return ContextCarrier.get(OPERATOR_KEY);
    }
}
