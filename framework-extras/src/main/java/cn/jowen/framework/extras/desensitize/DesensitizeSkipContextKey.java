package cn.jowen.framework.extras.desensitize;

import cn.jowen.framework.core.context.ContextKey;
import org.jspecify.annotations.NullMarked;

/**
 * 脱敏跳过开关上下文键。
 *
 * <p>在序列化期间（如管理员查看明细）将本键设为 {@code true}，
 * {@link cn.jowen.framework.extras.desensitize.serializer.DesensitizeJsonSerializer}
 * 会跳过全部字段脱敏，原样输出明文；作用域结束（{@code runWith} 退出）自动恢复。
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class DesensitizeSkipContextKey {

    /**
     * 全局跳过脱敏开关：值为 {@code true} 时序列化不脱敏。
     */
    public static final ContextKey<Boolean> DESENSITIZE_SKIP =
            ContextKey.named("desensitize.skip", Boolean.class);

    private DesensitizeSkipContextKey() {
    }
}
