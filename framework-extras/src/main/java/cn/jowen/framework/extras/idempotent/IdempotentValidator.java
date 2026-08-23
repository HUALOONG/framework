package cn.jowen.framework.extras.idempotent;

import org.jspecify.annotations.NullMarked;

/**
 * 幂等校验器接口。
 *
 * <p>支持 Token 模式（请求头携带 Token，校验后删除）与 Key 模式（SETNX 原子操作）。
 * 具体实现由 {@code RedisIdempotentValidator}（分布式）或 {@code LocalIdempotentValidator}（单机兜底）提供。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public interface IdempotentValidator {

    /**
     * 校验幂等性：Token 模式下校验 Token 存在并删除；Key 模式下尝试 SETNX。
     *
     * @param key 业务唯一键
     * @return true 表示幂等校验通过，可继续执行
     */
    boolean validate(String key);

    /**
     * 标记请求已处理（Token 模式：已校验并删除；Key 模式：SETNX 成功后标记）。
     *
     * @param key 业务唯一键
     */
    void mark(String key);

    /**
     * 移除标记（异常回滚时调用）。
     *
     * @param key 业务唯一键
     */
    void remove(String key);
}
