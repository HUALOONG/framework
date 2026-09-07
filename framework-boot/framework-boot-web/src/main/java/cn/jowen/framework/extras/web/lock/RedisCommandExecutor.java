package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Redis 命令执行抽象：屏蔽 Jedis / Lettuce / StringRedisTemplate 差异。
 *
 * <p>框架不强制依赖任何 Redis 客户端，由业务方注入适配实现。
 *
 * <p><b>脚本能力是可选的</b>：{@link #supportsScript()} 与 {@link #eval(String, List, List)}
 * 均为 {@code default} 方法，既有的四方法实现不重写也能编译、运行，行为与扩展前完全一致。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.2
 */
@NullMarked
public interface RedisCommandExecutor {

    /**
     * 尝试设置键值（仅当键不存在时），并指定过期时间。
     *
     * @param key        键
     * @param value      值
     * @param expireMillis 过期毫秒数
     * @return 设置成功返回 {@code true}，键已存在返回 {@code false}
     */
    boolean setIfAbsent(String key, String value, long expireMillis);

    /**
     * 读取键值。
     *
     * @param key 键
     * @return 值，不存在返回 {@code null}
     */
    @Nullable String get(String key);

    /**
     * 删除键。
     *
     * @param key 键
     */
    void delete(String key);

    /**
     * 原子执行释放锁脚本：值匹配时删除（避免误删他人锁）。
     *
     * @param key   键
     * @param value 期望值
     * @return 是否释放成功
     */
    boolean deleteIfMatch(String key, String value);

    /**
     * 是否支持执行 Lua 脚本。
     *
     * <p>默认 {@code false}：既有的四方法实现无需改动即可继续工作，框架会按
     * “不支持脚本”处理并走本地降级路径，行为与扩展前完全一致。
     *
     * <p><b>契约要求</b>：实现方若重写了 {@link #eval(String, List, List)}，
     * <b>必须同时重写本方法并返回 {@code true}</b>；否则框架仍按“不支持脚本”处理，
     * 脚本能力等于没有生效（契约测试套件会强制校验这一致性）。
     *
     * @return 支持脚本执行返回 {@code true}，否则返回 {@code false}
     * @since 0.0.2
     */
    default boolean supportsScript() {
        return false;
    }

    /**
     * 原子执行一段 Lua 脚本。
     *
     * <p>默认实现抛出 {@link UnsupportedOperationException}，保证未实现脚本能力的
     * 适配器不会被静默降级为“什么都不做”。调用方应先通过 {@link #supportsScript()}
     * 探测，或在装配期一次性探测后决定降级路径。
     *
     * @param script Lua 脚本源码，不可为 {@code null} 或空白
     * @param keys   KEYS 数组，可为 {@link java.util.List#of()}（无 key 脚本），元素不可为 {@code null}
     * @param args   ARGV 数组，可为 {@link java.util.List#of()}，元素不可为 {@code null}
     * @return 脚本返回值；Redis Lua 的常见返回类型为 {@code Long} / {@code String} /
     *         {@code List<?>} / {@code null}，由调用方按脚本约定解析
     *         （建议配合 {@link RedisScriptReplies} 做类型收敛）
     * @throws UnsupportedOperationException 当前实现不支持脚本执行
     * @since 0.0.2
     */
    default @Nullable Object eval(String script, List<String> keys, List<String> args) {
        throw new UnsupportedOperationException(
                "RedisCommandExecutor [" + getClass().getName()
                        + "] does not support Lua script execution");
    }
}
