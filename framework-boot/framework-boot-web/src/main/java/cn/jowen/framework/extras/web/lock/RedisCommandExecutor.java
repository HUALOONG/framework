package cn.jowen.framework.extras.web.lock;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Redis 命令执行抽象：屏蔽 Jedis / Lettuce / StringRedisTemplate 差异。
 *
 * <p>框架不强制依赖任何 Redis 客户端，由业务方注入适配实现。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
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
}
