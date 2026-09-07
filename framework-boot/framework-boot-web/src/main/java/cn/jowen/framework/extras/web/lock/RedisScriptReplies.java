package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Redis Lua 脚本返回值的类型收敛工具（纯静态、无状态）。
 *
 * <p><b>存在意义</b>：同一段 Lua 脚本在不同 Redis 客户端下返回值的 Java 类型并不一致——
 * Redisson 用 {@code ReturnType.LONG} 时给 {@code Long}，用 {@code ReturnType.STATUS} 时
 * 可能给 {@code String} 或 {@code Boolean}，某些客户端在脚本无返回值时给 {@code null}。
 * 若每个调用点各写一套解析，容易出现“换个客户端就 NPE / 类型转换异常”的问题。
 *
 * <p>本工具把这些差异收敛到一处：统一转成 {@code long} 语义，再交给调用方判定。
 *
 * <p><b>典型用法</b>：限流脚本返回 {@code 1} 表示放行、{@code 0} 表示拒绝，
 * 调用方只需 {@code RedisScriptReplies.isAllowed(executor.eval(...))}。
 *
 * @author 王飞
 * @since 0.0.2
 * @version 0.0.2
 */
@NullMarked
public final class RedisScriptReplies {

    /**
     * 工具类禁止实例化。
     *
     * @throws IllegalStateException 总是抛出，本类仅提供静态方法
     */
    private RedisScriptReplies() {
        throw new IllegalStateException("RedisScriptReplies is a utility class and must not be instantiated");
    }

    /**
     * 将脚本返回值收敛为 {@code long}。
     *
     * <p>转换规则：
     * <ul>
     *   <li>{@code null} → {@code 0L}（脚本无返回值视为“零/否”）</li>
     *   <li>{@link Number} → {@link Number#longValue()}</li>
     *   <li>{@link String} → {@link Long#parseLong(String)}</li>
     *   <li>{@link Boolean} → {@code true} 为 {@code 1L}，{@code false} 为 {@code 0L}</li>
     * </ul>
     *
     * @param reply 脚本返回值，可为 {@code null}
     * @return 收敛后的 long 值
     * @throws IllegalArgumentException reply 不是上述受支持的类型，或字符串无法解析为 long
     */
    public static long toLong(@Nullable Object reply) {
        if (reply == null) {
            return 0L;
        }
        if (reply instanceof Number number) {
            return number.longValue();
        }
        if (reply instanceof String text) {
            return Long.parseLong(text);
        }
        if (reply instanceof Boolean flag) {
            return flag ? 1L : 0L;
        }
        throw new IllegalArgumentException(
                "Unsupported Redis script reply type [" + reply.getClass().getName() + "]");
    }

    /**
     * 判定脚本返回值是否表示“放行”。
     *
     * <p>约定：脚本返回大于 0 的数值表示放行，0 或负值表示拒绝，{@code null} 视为拒绝。
     *
     * @param reply 脚本返回值，可为 {@code null}
     * @return 放行返回 {@code true}，拒绝返回 {@code false}
     * @throws IllegalArgumentException reply 类型不受支持，参见 {@link #toLong(Object)}
     */
    public static boolean isAllowed(@Nullable Object reply) {
        return toLong(reply) > 0L;
    }

    /**
     * 将脚本返回值收敛为字符串，便于日志与诊断。
     *
     * @param reply 脚本返回值，可为 {@code null}
     * @return 字符串形式；{@code null} 输入返回 {@code null}
     */
    public static @Nullable String toReplyString(@Nullable Object reply) {
        return reply == null ? null : String.valueOf(reply);
    }
}
